package com.web3auth.wallet.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.web3auth.wallet.api.ApiHelper
import com.web3auth.wallet.api.Web3AuthApi
import com.web3auth.wallet.api.models.EthGasAPIResponse
import com.web3auth.wallet.api.models.GasApiResponse
import com.web3auth.wallet.api.models.Params
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.web3j.crypto.*
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.response.EthGetBalance
import org.web3j.protocol.core.methods.response.EthGetTransactionCount
import org.web3j.protocol.core.methods.response.EthSendTransaction
import org.web3j.protocol.core.methods.response.Web3ClientVersion
import org.web3j.protocol.http.HttpService
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import kotlin.Pair

class EthereumViewModel : ViewModel() {

    private lateinit var web3: Web3j
    private lateinit var web3Balance: EthGetBalance
    var isWeb3Configured = MutableLiveData(false)
    var priceInUSD = MutableLiveData("")
    var publicAddress = MutableLiveData("")
    var balance = MutableLiveData(0.0)
    var ethGasAPIResponse: MutableLiveData<EthGasAPIResponse> = MutableLiveData(null)
    var transactionHash: MutableLiveData<Pair<Boolean, String>> = MutableLiveData(Pair(false, ""))
    var gasAPIResponse: MutableLiveData<GasApiResponse> = MutableLiveData(null)

    init {
        configureWeb3j()
        getMaxTransactionConfig()
    }

    private fun configureWeb3j() {
        val url =
            "https://rpc-mumbai.maticvigil.com/" // Mainnet: https://mainnet.infura.io/v3/{}, 7f287687b3d049e2bea7b64869ee30a3
        web3 = Web3j.build(HttpService(url))
        try {
            val clientVersion: Web3ClientVersion = web3.web3ClientVersion().sendAsync().get()
            isWeb3Configured.value = !clientVersion.hasError()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getCurrencyPriceInUSD(fsym: String, tsyms: String) {
        GlobalScope.launch {
            val web3AuthApi = ApiHelper.getTorusInstance().create(Web3AuthApi::class.java)
            val result = web3AuthApi.getCurrencyPrice(fsym, tsyms)
            if (result.isSuccessful && result.body() != null) {
                priceInUSD.postValue(result.body()?.USD)
            }
        }
    }

    fun getPublicAddress(sessionId: String) {
        GlobalScope.launch {
            val credentials: Credentials = Credentials.create(sessionId)
            publicAddress.postValue(credentials.address)
        }
    }

    fun retrieveBalance(publicAddress: String) {
        GlobalScope.launch {
            web3Balance = web3.ethGetBalance(publicAddress, DefaultBlockParameterName.LATEST)
                .sendAsync()
                .get()
            balance.postValue(web3Balance.balance.toDouble())
        }
    }

    fun getSignature(privateKey: String, message: String): String {
        val credentials: Credentials = Credentials.create(privateKey)
        val hashedData = Hash.sha3(message.toByteArray(StandardCharsets.UTF_8))
        val signature = Sign.signMessage(hashedData, credentials.ecKeyPair)
        val r = Numeric.toHexString(signature.r)
        val s = Numeric.toHexString(signature.s).substring(2)
        val v = Numeric.toHexString(signature.v).substring(2)
        return StringBuilder(r).append(s).append(v).toString()
    }

    private fun getMaxTransactionConfig() {
        GlobalScope.launch {
            val web3AuthApi = ApiHelper.getEthInstance().create(Web3AuthApi::class.java)
            val result = web3AuthApi.getMaxTransactionConfig()
            if (result.isSuccessful && result.body() != null) {
                ethGasAPIResponse.postValue(result.body() as EthGasAPIResponse)
            }
        }
    }

    fun getGasConfig() {
        GlobalScope.launch {
            val web3AuthApi = ApiHelper.getMockGasInstance().create(Web3AuthApi::class.java)
            val result = web3AuthApi.getGasConfig()
            if (result.isSuccessful && result.body() != null) {
                gasAPIResponse.postValue(result.body() as GasApiResponse)
            }
        }
    }

    fun sendTransaction(
        privateKey: String,
        recipientAddress: String,
        amountToBeSent: Double,
        data: String?,
        params: Params
    ) {
        GlobalScope.launch {
            try {
                val credentials: Credentials = Credentials.create(privateKey)
                val ethGetTransactionCount: EthGetTransactionCount = web3.ethGetTransactionCount(
                    credentials.address,
                    DefaultBlockParameterName.LATEST
                ).send()
                val nonce: BigInteger = ethGetTransactionCount.transactionCount
                val value: BigInteger =
                    Convert.toWei(amountToBeSent.toString(), Convert.Unit.ETHER).toBigInteger()
                val gasLimit: BigInteger = BigInteger.valueOf(21000)

                val rawTransaction: RawTransaction = RawTransaction.createTransaction(
                    80001,
                    nonce,
                    gasLimit,
                    recipientAddress,
                    value,
                    data ?: "",
                    BigInteger.valueOf(params.suggestedMaxPriorityFeePerGas?.toLong() ?: 7),
                    BigInteger.valueOf(params.suggestedMaxFeePerGas?.toLong() ?: 70)
                )
                // Sign the transaction
                val signedMessage: ByteArray =
                    TransactionEncoder.signMessage(rawTransaction, credentials)
                val hexValue: String = Numeric.toHexString(signedMessage)
                val ethSendTransaction: EthSendTransaction = web3.ethSendRawTransaction(hexValue).send()
                if(ethSendTransaction.error != null) {
                    transactionHash.postValue(Pair(false, ethSendTransaction.error.message))
                } else {
                    transactionHash.postValue(Pair(true, ethSendTransaction.transactionHash))
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}