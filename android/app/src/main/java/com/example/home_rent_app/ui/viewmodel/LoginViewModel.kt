package com.example.home_rent_app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.home_rent_app.data.datastore.DataStore.PreferenceKeys.USER_ID
import com.example.home_rent_app.data.dto.toJWT
import com.example.home_rent_app.data.dto.toUser
import com.example.home_rent_app.data.model.KakaoOauthRequest
import com.example.home_rent_app.data.model.NaverOauthRequest
import com.example.home_rent_app.data.model.UserProfileRequest
import com.example.home_rent_app.data.repository.login.LoginRepository
import com.example.home_rent_app.data.repository.loginProfile.LoginProfileRepository
import com.example.home_rent_app.data.repository.token.TokenRepository
import com.example.home_rent_app.util.Constants.GENDER_DEFAULT
import com.example.home_rent_app.util.logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginRepository: LoginRepository,
    private val tokenRepository: TokenRepository,
    private val loginProfileRepository: LoginProfileRepository
) : ViewModel() {

    private val _nickNameCheck: MutableSharedFlow<Boolean> = MutableSharedFlow()
    val nickNameCheck: SharedFlow<Boolean> get() = _nickNameCheck

    private val _imageUrl: MutableStateFlow<String> = MutableStateFlow("")
    val imageUrl: StateFlow<String> get() = _imageUrl

    private val _isLogin = MutableStateFlow(false)
    val isLogin: StateFlow<Boolean> get() = _isLogin

    private val _gender = MutableStateFlow<String>(GENDER_DEFAULT)
    val gender: StateFlow<String?> get() = _gender

    init {
        viewModelScope.launch {
            loginRepository.getIsLogin().collect { isLogin ->
                _isLogin.value = isLogin
                if (isLogin) { // 자동로그인이 되어있는 경우
                    setAppSession()
                    connectUser() // 채팅 관련
                    setUserSession(setUserId())
                }
            }
        }
    }

    // UserSession 에 UserId 저장
    private fun setUserSession(userId: Int) {
        viewModelScope.launch {
            loginRepository.setUserIdAtUserSession(userId)
        }
    }

    private fun setAppSession() {
        viewModelScope.launch {
            tokenRepository.getToken().collect {
                tokenRepository.setAppSession(it)
            }
        }
    }

    private fun connectUser() {
        viewModelScope.launch {
            loginRepository.connectUser().collect { data ->
            }
        }
    }

    // 유저 정보를 DataStore 에서 꺼내와 UserSession 에 저장
    private suspend fun setUserId(): Int {
        return withContext(Dispatchers.IO) {
            loginRepository.getUserId().first()[USER_ID] ?: 0
        }
    }

    fun getKakaoToken(kakaoOauthRequest: KakaoOauthRequest) {
        viewModelScope.launch {
            val response = loginRepository.getKakaoToken(kakaoOauthRequest)
            val jwt = response.toJWT()
            val user = response.toUser()
            launch {
                tokenRepository.saveToken(
                    listOf(
                        jwt.accessToken.tokenCode,
                        jwt.refreshToken.tokenCode
                    )
                )
                tokenRepository.getToken().collect {
                    tokenRepository.setAppSession(it)
                }
            }
            launch {
                // 유저 id는 서버에서 내려오므로 여기에서 DataStore 에 저장
                _gender.value = user.gender.orEmpty()
                val userId = user.userId ?: 0
                loginRepository.saveUserIDAtDataStore(userId)
                setUserSession(setUserId())
            }
        }
    }

    fun getNaverToken(naverOauthRequest: NaverOauthRequest) {
        viewModelScope.launch {
            val response = loginRepository.getNaverToken(naverOauthRequest)
            val jwt = response.toJWT()
            val user = response.toUser()
            launch {
                tokenRepository.saveToken(
                    listOf(
                        jwt.accessToken.tokenCode,
                        jwt.refreshToken.tokenCode
                    )
                )
                tokenRepository.getToken().collect {
                    tokenRepository.setAppSession(it)
                }
            }
            launch {
                // 유저 id는 서버에서 내려오므로 여기에서 DataStore 에 저장
                _gender.value = user.gender.orEmpty()
                val userId = user.userId ?: 0
                loginRepository.saveUserIDAtDataStore(userId)
                setUserSession(setUserId())
            }
        }
    }

    // 닉네임 중복 검사
    fun checkNickName(nickName: String) {
        viewModelScope.launch {
            _nickNameCheck.emit(loginProfileRepository.checkNickName(nickName).isDuplicated)
        }
    }

    // 유저 이미지를 MultipartBody 로 보내서 Url 로 받기
    fun getProfileImage(body: MultipartBody.Part) {
        viewModelScope.launch {
            val bodyList = mutableListOf<MultipartBody.Part>().apply { this.add(body) }
            loginProfileRepository.getImageUrl(bodyList).collect {
                _imageUrl.value = it.images.first()
            }
        }
    }

    // 유저 정보를 서버에 보내기
    fun setUserProfile(userId: Int, userProfileRequest: UserProfileRequest) {
        viewModelScope.launch {
            logger("Test UserId : $userId")
            loginProfileRepository.setUserProfile(userId, userProfileRequest)
        }
    }

    fun saveIsLogin() {
        viewModelScope.launch {
            loginRepository.saveIsLogin()
        }
    }
}
