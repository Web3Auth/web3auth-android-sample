package com.web3auth.wallet

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.os.postDelayed
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.web3auth.core.Web3Auth
import com.web3auth.core.getCustomTabsBrowsers
import com.web3auth.core.getDefaultBrowser
import com.web3auth.core.types.Web3AuthOptions
import com.web3auth.core.types.Web3AuthResponse
import com.web3auth.core.types.WhiteLabelData
import com.web3auth.wallet.api.ApiHelper
import com.web3auth.wallet.api.Web3AuthApi
import com.web3auth.wallet.utils.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.web3j.crypto.Credentials
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.response.EthGetBalance
import org.web3j.protocol.core.methods.response.EthGetTransactionCount
import org.web3j.protocol.core.methods.response.Web3ClientVersion
import org.web3j.protocol.http.HttpService
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import java.math.BigDecimal
import java.math.BigInteger
import java.text.DecimalFormat
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var web3Auth: Web3Auth
    private var web3AuthResponse: Web3AuthResponse? = null
    private lateinit var web3: Web3j
    private lateinit var credentials: Credentials
    private lateinit var publicAddress: String
    private lateinit var web3Balance: EthGetBalance
    private lateinit var selectedNetwork: String
    private lateinit var tvExchangeRate: AppCompatTextView
    private lateinit var tvPriceInUSD: AppCompatTextView
    private lateinit var priceInUSD: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()
        selectedNetwork =
            Web3AuthApp.getContext()?.web3AuthWalletPreferences?.getString(NETWORK, "Mainnet")
                .toString()
        configureWeb3j()
        configureWeb3Auth()
        setData()
        setUpListeners()
    }

    private fun configureWeb3Auth() {
        Web3Auth(
            Web3AuthOptions(
                context = this,
                clientId = getString(R.string.web3auth_project_id),
                network = NetworkUtils.getWebAuthNetwork(selectedNetwork),
                redirectUrl = Uri.parse("torusapp://org.torusresearch.web3authexample/redirect"),
                whiteLabel = WhiteLabelData(
                    "Web3Auth Sample App", null, null, "en", true,
                    hashMapOf(
                        "primary" to "#123456"
                    )
                )
            )
        ).also { web3Auth = it }

        web3Auth.setResultUrl(intent.data)
        web3AuthResponse =
            Web3AuthApp.getContext()?.web3AuthWalletPreferences?.getObject(LOGIN_RESPONSE)
        web3AuthResponse?.let { getEthAddress(it) }

        findViewById<AppCompatTextView>(R.id.tvName).text = "Welcome ".plus(
            web3AuthResponse?.userInfo?.name?.split(" ")?.get(0)
        ).plus("!")
        findViewById<AppCompatTextView>(R.id.tvEmail).text = web3AuthResponse?.userInfo?.email
    }

    private fun configureWeb3j() {
        val url =
            "https://rpc-mumbai.maticvigil.com/" // Mainnet: https://mainnet.infura.io/v3/{}, 7f287687b3d049e2bea7b64869ee30a3
        web3 = Web3j.build(HttpService(url))
        try {
            val clientVersion: Web3ClientVersion = web3.web3ClientVersion().sendAsync().get()
            if (clientVersion.hasError()) {
                toast("Error connecting to Web3j")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setUpListeners() {
        tvExchangeRate = findViewById(R.id.tvExchangeRate)
        tvPriceInUSD = findViewById(R.id.tvPriceInUSD)
        findViewById<AppCompatButton>(R.id.btnTransfer).setOnClickListener {
            startActivity(Intent(this@MainActivity, TransferAssetsActivity::class.java))
        }
        findViewById<AppCompatImageView>(R.id.ivLogout).setOnClickListener { logout() }
        findViewById<AppCompatTextView>(R.id.tvLogout).setOnClickListener { logout() }
        findViewById<AppCompatImageView>(R.id.ivQRCode).setOnClickListener {
            showQRDialog(publicAddress)
        }

        findViewById<AppCompatButton>(R.id.btnSign).setOnClickListener {
            //signMessage()
            showSignTransactionDialog(false, "")
        }
    }

    private fun setData() {
        val blockChain =
            Web3AuthApp.getContext()?.web3AuthWalletPreferences?.getString(BLOCKCHAIN, "Ethereum")
        findViewById<AppCompatTextView>(R.id.tvNetwork).text =
            blockChain.plus(" ").plus(selectedNetwork)

        findViewById<AppCompatTextView>(R.id.tvViewTransactionStatus).setOnClickListener {
            openCustomTabs("https://mumbai.polygonscan.com/")
        }

        getCurrencyPriceInUSD("ETH", "USD")

        /*if(blockChain == getString(R.string.ethereum)) {
            EthManager.configureWeb3j()
        } else {
            SolanaManager.createWallet(NetworkUtils.getSolanaNetwork(selectedNetwork))
        }*/
    }

    private fun getEthAddress(web3AuthResponse: Web3AuthResponse) {
        val authArgs = CustomAuthArgs(NetworkUtils.getTorusNetwork(selectedNetwork))
        authArgs.networkUrl =
            "https://small-long-brook.ropsten.quiknode.pro/e2fd2eb01412e80623787d1c40094465aa67624a"
        // Initialize CustomAuth
        var torusSdk = CustomAuth(authArgs)
        val verifier = web3AuthResponse.userInfo?.verifier
        val verifierId = web3AuthResponse.userInfo?.verifierId

        Executors.newSingleThreadExecutor().execute {
            publicAddress = torusSdk.getEthAddress(verifier, verifierId)
            runOnUiThread {
                findViewById<AppCompatTextView>(R.id.tvAddress).text =
                    publicAddress.take(3).plus("...").plus(publicAddress.takeLast(4))
                Web3AuthApp.getContext()?.web3AuthWalletPreferences?.set(ETH_Address, publicAddress)
                retrieveBalance(publicAddress)
                Web3AuthApp.getContext()?.web3AuthWalletPreferences?.set(PUBLICKEY, publicAddress)
            }
        }
    }

    private fun getCurrencyPriceInUSD(fsym: String, tsyms: String) {
        GlobalScope.launch {
            val web3AuthApi = ApiHelper.getInstance(ApiHelper.baseUrl).create(Web3AuthApi::class.java)
            val result = web3AuthApi.getCurrencyPrice(fsym, tsyms)
            if(result.isSuccessful && result.body() != null) {
                Handler(Looper.getMainLooper()).postDelayed(10) {
                    priceInUSD = result.body()?.USD.toString()
                    tvExchangeRate.text = "1 ".plus(fsym).plus(" = ").plus(priceInUSD).plus(" $tsyms")
                }
            }
        }
    }

    private fun retrieveBalance(publicAddress: String) {
        Executors.newSingleThreadExecutor().execute {
            try {
                web3Balance = web3.ethGetBalance(
                    publicAddress,
                    DefaultBlockParameterName.LATEST
                ).sendAsync()
                    .get()
            } catch (e: Exception) {
                toast("balance failed")
            }
            runOnUiThread {
                val tvBalance = findViewById<AppCompatTextView>(R.id.tvBalance)
                tvBalance.text = Web3AuthUtils.toEther(web3Balance).toString()
                val usdPrice = BigDecimal(web3Balance.balance.toDouble()).multiply(BigDecimal(priceInUSD))/Web3AuthUtils.getEtherInWei().toBigDecimal()
                tvPriceInUSD.text = "= ".plus(usdPrice).plus(" USD")
            }
        }
    }

    private fun signMessage(privateKey: String, recipientAddress: String, amountToBeSent: Double) {
        try {
            val credentials: Credentials = Credentials.create(privateKey)
            println("Account address: " + credentials.address)
            println(
                "Balance: " + Convert.fromWei(
                    web3.ethGetBalance(credentials.address, DefaultBlockParameterName.LATEST)
                        .send().balance.toString(), Convert.Unit.ETHER
                )
            )
            val ethGetTransactionCount: EthGetTransactionCount = web3
                .ethGetTransactionCount(credentials.address, DefaultBlockParameterName.LATEST)
                .send()
            val nonce: BigInteger = ethGetTransactionCount.transactionCount
            val value: BigInteger =
                Convert.toWei(amountToBeSent.toString(), Convert.Unit.ETHER).toBigInteger()
            val gasLimit: BigInteger = BigInteger.valueOf(21000)
            val gasPrice: BigInteger = Convert.toWei("1", Convert.Unit.GWEI).toBigInteger()

            val rawTransaction: RawTransaction = RawTransaction.createEtherTransaction(
                nonce, gasPrice, gasLimit,
                recipientAddress, value
            )
            // Sign the transaction
            val signedMessage: ByteArray =
                TransactionEncoder.signMessage(rawTransaction, credentials)
            val hexValue: String = Numeric.toHexString(signedMessage)
            println("kexValue: $hexValue")
            // val ethSendTransaction: EthSendTransaction = web3.ethSendRawTransaction(hexValue).send()
            // val transactionHash: String = ethSendTransaction.transactionHash
            // println("transactionHash: $transactionHash")
            showSignTransactionDialog(true, hexValue)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun showQRDialog(publicAddress: String) {
        val dialog = Dialog(this@MainActivity)
        dialog.setContentView(R.layout.dialog_qr_code)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(false)
        dialog.window?.attributes?.windowAnimations = R.style.animation
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val ivQR = dialog.findViewById<AppCompatImageView>(R.id.ivQRCode)
        val tvAddress = dialog.findViewById<AppCompatTextView>(R.id.tvAddress)
        val ivClose = dialog.findViewById<AppCompatImageView>(R.id.ivClose)

        tvAddress.text = publicAddress
        tvAddress.setOnClickListener { copyToClipboard(tvAddress.text.toString()) }

        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(publicAddress, BarcodeFormat.QR_CODE, 200, 200)
        val w = bitMatrix.width
        val h = bitMatrix.height
        val pixels = IntArray(w * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                pixels[y * w + x] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
            }
        }
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
        ivQR.setImageBitmap(bitmap)

        ivClose.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showSignTransactionDialog(isSuccess: Boolean, ethHash: String?) {
        val dialog = Dialog(this@MainActivity)
        dialog.setContentView(R.layout.popup_sign_transaction)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.animation
        val ivState = dialog.findViewById<AppCompatImageView>(R.id.ivState)
        val transactionState = dialog.findViewById<AppCompatTextView>(R.id.tvTransactionState)
        val transactionHash = dialog.findViewById<AppCompatTextView>(R.id.tvTransactionHash)
        val tvCopy = dialog.findViewById<AppCompatTextView>(R.id.tvCopy)
        val ivClose = dialog.findViewById<AppCompatImageView>(R.id.ivClose)

        if (isSuccess) {
            transactionHash.text = ethHash
            transactionState.text = getString(R.string.sign_success)
            ivState.setImageDrawable(getDrawable(R.drawable.ic_iv_transaction_success))
            tvCopy.setOnClickListener { copyToClipboard(transactionHash.text.toString()) }
        } else {
            transactionState.text = getString(R.string.sign_failed)
            ivState.setImageDrawable(getDrawable(R.drawable.ic_transaction_failed))
            transactionHash.hide()
            tvCopy.hide()
        }

        ivClose.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showTransactionDialog(transactionStatus: TransactionStatus) {
        val dialog = Dialog(this@MainActivity)
        dialog.setContentView(R.layout.popup_transaction)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.animation
        val ivState = dialog.findViewById<AppCompatImageView>(R.id.ivState)
        val transactionState = dialog.findViewById<AppCompatTextView>(R.id.tvTransactionState)
        val tvStatus = dialog.findViewById<AppCompatTextView>(R.id.tvStatus)
        val ivClose = dialog.findViewById<AppCompatImageView>(R.id.ivClose)

        when (transactionStatus) {
            TransactionStatus.PLACED -> {
                transactionState.text = getString(R.string.transaction_placed)
                ivState.setImageDrawable(getDrawable(R.drawable.ic_transaction_placed))
                tvStatus.hide()
            }
            TransactionStatus.SUCCESSFUL -> {
                transactionState.text = getString(R.string.transaction_success)
                ivState.setImageDrawable(getDrawable(R.drawable.ic_iv_transaction_success))
            }
            TransactionStatus.FAILED -> {
                transactionState.text = getString(R.string.transaction_failed)
                ivState.setImageDrawable(getDrawable(R.drawable.ic_transaction_failed))
                tvStatus.text = getString(R.string.try_again)
            }
            else -> {
                transactionState.text = getString(R.string.transaction_pending)
                ivState.setImageDrawable(getDrawable(R.drawable.ic_transaction_pending))
            }
        }

        ivClose.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun copyToClipboard(text: String) {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("text", text)
        clipboardManager.setPrimaryClip(clipData)
        toast("Text Copied")
    }

    private fun openCustomTabs(url: String) {
        val defaultBrowser = this@MainActivity.getDefaultBrowser()
        val customTabsBrowsers = this@MainActivity.getCustomTabsBrowsers()

        if (customTabsBrowsers.contains(defaultBrowser)) {
            val customTabs = CustomTabsIntent.Builder().build()
            customTabs.intent.setPackage(defaultBrowser)
            customTabs.launchUrl(this@MainActivity, Uri.parse(url))
        } else if (customTabsBrowsers.isNotEmpty()) {
            val customTabs = CustomTabsIntent.Builder().build()
            customTabs.intent.setPackage(customTabsBrowsers[0])
            customTabs.launchUrl(this@MainActivity, Uri.parse(url))
        }
    }

    private fun logout() {
        val logoutCompletableFuture = web3Auth.logout()
        logoutCompletableFuture.whenComplete { _, error ->
            if (error == null) {
                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                finish()
            } else {
                Log.d("MainActivity_Web3Auth", error.message ?: "Something went wrong")
            }
        }
    }
}