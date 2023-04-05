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

    fun getClientIdByNetwork(network: String): String {
        return when (network) {
            "Mainnet" -> "BE4QJC39vkx56M_CaOZFGYuTKve17TpYta9ABSjHWBS_Z1MOMOhOYnjrQDT9YGXJXZvSXM6JULzzukqUB_7a5X0"
            "Testnet" -> "BKWc-6_pz5wgoZ5jvmgvbytxt7A8dvTTgsByZ87b8f-7NZW5zdhbznxT2MWJYJEv_O6MClj-g_HS4lYPJ4uQFhk"
            "Cyan" -> "BA5akJpGy6j5bVNL33RKpe64AXTiPGTSCYOI0i-BbDtbOYWtFQNdLzaC-WKibRtQ0sV_TVHC42TdOTbyZXdN-XI"
            else -> "BA5akJpGy6j5bVNL33RKpe64AXTiPGTSCYOI0i-BbDtbOYWtFQNdLzaC-WKibRtQ0sV_TVHC42TdOTbyZXdN-XI"
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