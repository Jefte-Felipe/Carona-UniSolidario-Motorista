package com.tcc.unisolidariamotorista.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.tcc.unisolidariamotorista.providers.BookingProvider

class CancelReceiver: BroadcastReceiver() {

    val bookingProvider = BookingProvider()

    override fun onReceive(context: Context, intent: Intent) {

        val idBooking = intent.extras?.getString("idBooking")
        cancelBooking(idBooking!!)
    }

    private fun cancelBooking(idBooking: String) {
        bookingProvider.updateStatus(idBooking, "cancel").addOnCompleteListener {

            if (it.isSuccessful) {
                Log.d("RECEIVER", "A VIAGEM FOI CANCELADAO")
            }
            else {
                Log.d("RECEIVER", "O STATUS DA VIAGEM NÃO PODE SER ATUALIZADO")
            }
        }
    }

}