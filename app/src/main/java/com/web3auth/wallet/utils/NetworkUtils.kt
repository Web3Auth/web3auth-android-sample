package com.web3auth.wallet.utils

import com.web3auth.core.Web3Auth
import org.p2p.solanaj.rpc.Cluster

object NetworkUtils {

    fun getWebAuthNetwork(network: String): Web3Auth.Network {
        return when (network) {
            "Mainnet" -> Web3Auth.Network.MAINNET
            "Testnet" -> Web3Auth.Network.TESTNET
            "Cyan" -> Web3Auth.Network.CYAN
            else -> Web3Auth.Network.MAINNET
        }
    }

    fun getSolanaNetwork(network: String): Cluster {
        return when (network) {
            "Solana Mainnet" -> Cluster.MAINNET
            "Solana Testnet" -> Cluster.TESTNET
            "Solana Devnet" -> Cluster.DEVNET
            else -> Cluster.MAINNET
        }
    }

    fun getRpcUrl(blockChain: String): String {
        return when (blockChain) {
            "ETH Mainnet" -> "https://mainnet.infura.io/v3/7f287687b3d049e2bea7b64869ee30a3"
            "Solana Testnet" -> "SOL"
            "Solana Mainnet" -> "SOL"
            "Solana Devnet" -> "SOL"
            "Polygon Mainnet" -> "https://rpc.ankr.com/polygon" // https://rpc-mumbai.maticvigil.com/
            "Binance Mainnet" -> "https://rpc.ankr.com/bsc"
            "ETH Goerli" -> "https://goerli.infura.io/v3/7f287687b3d049e2bea7b64869ee30a3"
            else -> "https://mainnet.infura.io/v3/7f287687b3d049e2bea7b64869ee30a3"
        }
    }
}