package com.example.home_rent_app.ui.wanthome.detail

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.example.home_rent_app.R
import com.example.home_rent_app.databinding.ActivityWantHomeDetailBinding
import com.example.home_rent_app.ui.viewmodel.WantHomeViewModel
import com.example.home_rent_app.ui.wanthome.WantHomeActivity
import com.example.home_rent_app.ui.wanthomeresult.WantHomeResultActivity
import com.example.home_rent_app.util.ItemIdSession
import com.example.home_rent_app.util.UserSession
import com.example.home_rent_app.util.collectStateFlow
import com.example.home_rent_app.util.logger
import com.example.home_rent_app.util.setLikeClickEvent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WantHomeDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWantHomeDetailBinding
    private val viewModel: WantHomeViewModel by viewModels()
    @Inject
    lateinit var idSession: ItemIdSession
    @Inject
    lateinit var userSession: UserSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_want_home_detail)
        binding.lifecycleOwner = this
        binding.vm = viewModel
        goHomeActivity()
        clickLikeButton()
        getWantHomeDetail()
        checkMyItem()
        setGoChat()
    }

    private fun goHomeActivity() {
        binding.btnGoToHome.setOnClickListener {
            onBackPressed()
        }
    }

    private fun clickLikeButton() {
        binding.btnLike.setLikeClickEvent(lifecycleScope) {
            binding.btnLike.isSelected = binding.btnLike.isSelected != true
        }
    }

    private fun getWantHomeDetail() {
        idSession.itemId?.let { viewModel.getWantHomeDetail(it) }
    }

    private fun checkMyItem() {
        collectStateFlow(viewModel.wantHomeDetail) { Response ->
            if (Response?.user?.userId == userSession.userId) {
                binding.btnLike.visibility = View.GONE
                binding.btnGoChat.visibility = View.GONE
            }
        }
    }

    private fun setGoChat() {
        binding.btnGoChat.setOnClickListener {
            viewModel.joinNewChannel()
        }
    }
}
