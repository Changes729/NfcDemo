package com.andresjakl.nfcdemo.hce

import android.nfc.cardemulation.HostApduService
import android.os.Bundle

class HostCardEmulatorService : HostApduService() {
    val MIN_APDU_LENGTH = 12

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        val ret : ByteArray;
        if (commandApdu == null) {
            ret = byteArrayOf(0x90.toByte(), 0x00);
        }
        else if(commandApdu.size < MIN_APDU_LENGTH)
        {
            ret = byteArrayOf(0x6F, 0x00);
        }
        else if(commandApdu.indexOf(0) != 0x00)
        {
            ret = byteArrayOf(0x6E, 0x00);
        }
        else if(commandApdu.indexOf(1) != 0xA4)
        {
            ret = byteArrayOf(0x6D, 0x00);
        }
        else
        {
            val AID = byteArrayOf(0xA0.toByte(),0x00,0x00,0x02,0x47,0x10,0x01);
            val commandOffset = 5;
            var isAID = true;
            for(i in 5 .. 11)
            {
                if(AID.indexOf((i - commandOffset).toByte()) != commandApdu.indexOf(i.toByte()))
                {
                    isAID = false;
                    break;
                }
            }

            if(isAID)
            {
                ret = byteArrayOf(0x90.toByte(), 0x00);
            }
            else
            {
                ret = byteArrayOf(0x6F, 0x00);
            }
        }

        return ret;
    }

    override fun onDeactivated(reason: Int) {
        TODO("Not yet implemented")
    }
}