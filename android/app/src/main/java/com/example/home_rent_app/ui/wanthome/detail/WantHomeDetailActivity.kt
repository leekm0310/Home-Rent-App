package com.example.home_rent_app.ui.wanthome.detail

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.example.home_rent_app.R
import com.example.home_rent_app.databinding.ActivityWantHomeDetailBinding
import com.example.home_rent_app.ui.viewmodel.WantHomeViewModel
import com.example.home_rent_app.ui.wanthome.WantHomeActivity
import com.example.home_rent_app.util.ItemIdSession
import com.example.home_rent_app.util.setLikeClickEvent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WantHomeDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWantHomeDetailBinding
    private val viewModel: WantHomeViewModel by viewModels()
    @Inject
    lateinit var idSession: ItemIdSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_want_home_detail)
        binding.lifecycleOwner = this
        binding.vm = viewModel
        goHomeActivity()
        clickLikeButton()
        getWantHomeDetail()
    }

    private fun goHomeActivity() {
        binding.btnGoToHome.setOnClickListener {
            val intent = Intent(this, WantHomeActivity::class.java)
            startActivity(intent)
            finish()
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
}
