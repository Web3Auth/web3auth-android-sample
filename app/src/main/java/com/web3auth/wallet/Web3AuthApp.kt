package com.web3auth.wallet

import android.app.Application
import android.content.Context
import com.web3auth.wallet.utils.LocaleUtils

class Web3AuthApp : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleUtils.onAttach(base, "en"))
    }
}