package com.web3auth.wallet.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.web3auth.wallet.api.ApiHelper
import com.web3auth.wallet.api.Web3AuthApi
import com.web3auth.wallet.utils.MemoProgram
import com.web3auth.wallet.utils.SingleLiveEvent
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.bitcoinj.core.Base58
import org.p2p.solanaj.core.PublicKey
import org.p2p.solanaj.core.Transaction
import org.p2p.solanaj.programs.SystemProgram.transfer
import org.p2p.solanaj.rpc.Cluster
import org.p2p.solanaj.rpc.RpcClient
import java.math.BigInteger

class SolanaViewModel : ViewModel() {

    private lateinit var client: RpcClient
    private lateinit var account: org.p2p.solanaj.core.Account
    var priceInUSD = MutableLiveData("")
    var publicAddress = MutableLiveData("")
    var privateKey = MutableLiveData("")
    var balance = MutableLiveData(0L)
    var signature = SingleLiveEvent<String?>()
    var sendTransactionResult = MutableLiveData(Pair(false, ""))
    var error: MutableLiveData<Boolean> = MutableLiveData(false)

    fun setNetwork(cluster: Cluster, ed25519Key: String) {
        client = RpcClient(cluster)
        account = org.p2p.solanaj.core.Account(BigInteger(ed25519Key, 16).toByteArray())
    }

    fun getPublicAddress() {
        try {
            publicAddress.postValue(account.publicKey.toBase58())
            privateKey.postValue(Base58.encode(account.secretKey))
        } catch (ex: Exception) {
            error.postValue(true)
            ex.printStackTrace()
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun getCurrencyPriceInUSD(fsym: String, tsyms: String) {
        GlobalScope.launch {
            val web3AuthApi = ApiHelper.getTorusInstance().create(Web3AuthApi::class.java)
            val result = web3AuthApi.getCurrencyPrice(fsym, tsyms)
            if (result.isSuccessful && result.body() != null) {
                priceInUSD.postValue(result.body()?.USD)
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun getBalance(publicAddress: String) {
        GlobalScope.launch {
            balance.postValue(client.api.getBalance(PublicKey(publicAddress)))
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun signTransaction(message: String) {
        GlobalScope.launch {
            val transaction = Transaction()
            transaction.addInstruction(
                MemoProgram.writeUtf8(
                    PublicKey(account.publicKey.toBase58()),
                    message
                )
            )
            try {
                signature.postValue(client.api.sendTransaction(transaction, account))
            } catch (ex: Exception) {
                signature.postValue("error")
                ex.printStackTrace()
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun signAndSendTransaction(
        fromKey: String, toKey: String, amount: Long, message: String?
    ) {
        GlobalScope.launch {
            val fromPublicKey = PublicKey(fromKey)
            val toPublicKey = PublicKey(toKey)
            val transaction = Transaction()
            transaction.addInstruction(transfer(fromPublicKey, toPublicKey, amount))
            if (message?.isNotEmpty() == true) {
                transaction.addInstruction(MemoProgram.writeUtf8(account.publicKey, message))
            }
            try {
                val result = client.api.sendTransaction(transaction, account)
                if (result.isNotEmpty()) {
                    sendTransactionResult.postValue(Pair(true, result))
                }
            } catch (ex: Exception) {
                sendTransactionResult.postValue(Pair(false, "error"))
                ex.printStackTrace()
            }
        }
    }
}