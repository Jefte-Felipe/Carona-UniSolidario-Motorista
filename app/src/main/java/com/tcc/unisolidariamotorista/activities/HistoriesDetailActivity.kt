package com.tcc.unisolidariamotorista.activities

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.tcc.unisolidariamotorista.databinding.ActivityHistoryDetailBinding
import com.tcc.unisolidariamotorista.models.Client
import com.tcc.unisolidariamotorista.models.History
import com.tcc.unisolidariamotorista.providers.ClientProvider
import com.tcc.unisolidariamotorista.providers.HistoryProvider
import com.tcc.unisolidariamotorista.utils.RelativeTime

class HistoriesDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHistoryDetailBinding
    private var historyProvider = HistoryProvider()
    private var clientProvider = ClientProvider()
    private var extraId = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        extraId = intent.getStringExtra("id")!!
        getHistory()

        binding.imageViewBack.setOnClickListener { finish() }
    }

    private fun getHistory() {
        historyProvider.getHistoryById(extraId).addOnSuccessListener { document ->

            if (document.exists()) {
                val history = document.toObject(History::class.java)
                binding.textViewOrigin.text = history?.origin
                binding.textViewDestination.text = history?.destination
                binding.textViewDate.text = RelativeTime.getTimeAgo(history?.timestamp!!, this@HistoriesDetailActivity)
                binding.textViewPrice.text = "${String.format("%.1f", history?.price)}$"
                binding.textViewMyCalification.text = history?.calificationToDriver?.toString() ?: "Não foi informado"
                binding.textViewClientCalification.text = history?.calificationToClient?.toString() ?: "Não foi informado pelo aluno"
                binding.textViewTimeAndDistance.text = "${history?.time} Min - ${String.format("%.1f", history?.km)} Km"
                getClientInfo(history?.idClient!!)
            }

        }
    }

    private fun getClientInfo(id: String) {
        clientProvider.getClientById(id).addOnSuccessListener { document ->
            if (document.exists()) {
                val client = document.toObject(Client::class.java)
                binding.textViewEmail.text = client?.email
                binding.textViewName.text = "${client?.name} ${client?.lastname}"
                if (client?.image != null) {
                    if (client?.image != "") {
                        Glide.with(this).load(client?.image).into(binding.circleImageProfile)
                    }
                }
            }
        }
    }
}