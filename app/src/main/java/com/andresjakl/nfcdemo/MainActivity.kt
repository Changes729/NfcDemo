/*
 * MIT License
 *
 * Copyright (c) 2018 Andreas Jakl, https://www.andreasjakl.com/
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.andresjakl.nfcdemo

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.nfc.tech.Ndef
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.Html
import android.text.Spanned
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.nio.charset.Charset
import kotlin.experimental.and

class MainActivity : AppCompatActivity() {
    // NFC adapter for checking NFC state in the device
    private var nfcAdapter : NfcAdapter? = null

    // Pending intent for NFC intent foreground dispatch.
    // Used to read all NDEF tags while the app is running in the foreground.
    private var nfcPendingIntent: PendingIntent? = null
    // Optional: filter NDEF tags this app receives through the pending intent.
    //private var nfcIntentFilters: Array<IntentFilter>? = null

    private val KEY_LOG_TEXT = "logText"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Restore saved text if available
        if (savedInstanceState != null) {
            tv_messages.text = savedInstanceState.getCharSequence(KEY_LOG_TEXT)
        }

        // Check if NFC is supported and enabled
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        logMessage("NFC supported", (nfcAdapter != null).toString())
        logMessage("NFC enabled", (nfcAdapter?.isEnabled).toString())


        // Read all tags when app is running and in the foreground
        // Create a generic PendingIntent that will be deliver to this activity. The NFC stack
        // will fill in the intent with the details of the discovered tag before delivering to
        // this activity.
        nfcPendingIntent = PendingIntent.getActivity(this, 0,
                Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0)

        // Optional: Setup an intent filter from code for a specific NDEF intent
        // Use this code if you are only interested in a specific intent and don't want to
        // interfere with other NFC tags.
        // In this example, the code is commented out so that we get all NDEF messages,
        // in order to analyze different NDEF-formatted NFC tag contents.
        //val ndef = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
        //ndef.addCategory(Intent.CATEGORY_DEFAULT)
        //ndef.addDataScheme("https")
        //ndef.addDataAuthority("*.andreasjakl.com", null)
        //ndef.addDataPath("/", PatternMatcher.PATTERN_PREFIX)
        // More information: https://stackoverflow.com/questions/30642465/nfc-tag-is-not-discovered-for-action-ndef-discovered-action-even-if-it-contains
        //nfcIntentFilters = arrayOf(ndef)

        if (intent != null) {
            // Check if the app was started via an NDEF intent
            logMessage("Found intent in onCreate", intent.action.toString())
            processIntent(intent)
        }

        // Make sure the text view is scrolled down so that the latest messages are visible
        scrollDown()
    }

    override fun onResume() {
        super.onResume()
        // Get all NDEF discovered intents
        // Makes sure the app gets all discovered NDEF messages as long as it's in the foreground.
        nfcAdapter?.enableForegroundDispatch(this, nfcPendingIntent, null, null);
        // Alternative: only get specific HTTP NDEF intent
        //nfcAdapter?.enableForegroundDispatch(this, nfcPendingIntent, nfcIntentFilters, null);
    }

    override fun onPause() {
        super.onPause()
        // Disable foreground dispatch, as this activity is no longer in the foreground
        nfcAdapter?.disableForegroundDispatch(this);
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        logMessage("Found intent in onNewIntent", intent?.action.toString())
        // If we got an intent while the app is running, also check if it's a new NDEF message
        // that was discovered
        if (intent != null) processIntent(intent)
    }

    /**
     * Check if the Intent has the action "ACTION_NDEF_DISCOVERED". If yes, handle it
     * accordingly and parse the NDEF messages.
     * @param checkIntent the intent to parse and handle if it's the right type
     */
    private fun processIntent(checkIntent: Intent) {
        // Check if intent has the action of a discovered NFC tag
        // with NDEF formatted contents
        if (checkIntent.action == NfcAdapter.ACTION_NDEF_DISCOVERED) {
            logMessage("New NDEF intent", checkIntent.toString())

            // Retrieve the raw NDEF message from the tag
            val rawMessages = checkIntent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            logMessage("Raw messages", rawMessages.size.toString())

            // Complete variant: parse NDEF messages
            if (rawMessages != null) {
                val messages = arrayOfNulls<NdefMessage?>(rawMessages.size)
                for (i in rawMessages.indices) {
                    messages[i] = rawMessages[i] as NdefMessage;
                }
                // Process the messages array.
                processNdefMessages(messages)
            }
        }
        else if(checkIntent.action == NfcAdapter.ACTION_TECH_DISCOVERED)
        {
            // todo: check.
        }
        else if(checkIntent.action == NfcAdapter.ACTION_TAG_DISCOVERED)
        {
            val tag: Tag = checkIntent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            readTagId(tag);
            readTagData(tag);
//            updateTagData(tag);
            writeNdefToTag(tag);
        }
    }

    private fun writeNdefToTag(tag: Tag){
        val mimeRecord = NdefRecord.createMime("application/vnd.com.example.android.beam",
            "Beam me up, Android".toByteArray(Charset.forName("US-ASCII")))

        try {
            val ndef = Ndef.get(tag)
            ndef.connect()
            ndef.writeNdefMessage(NdefMessage(mimeRecord))
        } catch (e: IOException) {
            print(e.message)
        }
    }

    private fun updateTagData(tag: Tag) {
        if (tag.techList.contains(MifareClassic::class.java.name)) {
            val mifareTag = MifareClassic.get(tag)
            try {
                val BLOCK_INDEX = 1;
                mifareTag.connect()
                if (mifareTag.authenticateSectorWithKeyB(0, MifareClassic.KEY_DEFAULT)) {
                    var bytes = mifareTag.readBlock(BLOCK_INDEX);
                    logMessage("Update bytes:", byteArrayToHexString(bytes, " "));
                    bytes[0] = (bytes[0] + 1).toByte();
                    logMessage("To:", byteArrayToHexString(bytes, " "));
                    mifareTag.writeBlock(BLOCK_INDEX, bytes);
                    bytes = mifareTag.readBlock(BLOCK_INDEX);
                    logMessage("Check bytes:", byteArrayToHexString(bytes, " "));
                }
            } catch (e: IOException) {
                logMessage("Error:", e.message)
            }
            finally {
                mifareTag.close()
            }
        }
    }

    private fun readTagId(tag: Tag) {
        logMessage("New Tag", tag.toString() +  byteArrayToHexString(tag.id, " "));
    }

    private fun readTagData(tag: Tag) {
        if (tag.techList.contains(MifareClassic::class.java.name)) {
            val mifareTag = MifareClassic.get(tag);
            val stringBuilder = StringBuilder();
            var lastSector = -1;
            try {
                mifareTag.connect();
                for (index: Int in 0 until mifareTag.blockCount) {
                    val currentSector = mifareTag.blockToSector(index);
                    if (lastSector != currentSector) {
                        if (mifareTag.authenticateSectorWithKeyB(
                                currentSector,
                                MifareClassic.KEY_DEFAULT
                            )
                        ) {
                            lastSector = currentSector;
                            stringBuilder.append("Sector $currentSector <br>");
                            continue;
                        }

                        break;
                    }

                    stringBuilder.append(
                        "[" + byteArrayToHexString(
                            mifareTag.readBlock(index),
                            ","
                        ) + "]<br>"
                    );
                }
            } catch (e: IOException) {
                logMessage("Error:", e.message);
            }
            finally {
                mifareTag.close();
            }
            logMessage("Block Value:", stringBuilder.toString());
        }
    }

    private fun byteArrayToHexString(bytes: ByteArray, split: String = ""): String {
        val sb = StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02X", b and 0xff.toByte()))
            sb.append(split);
        }
        return sb.toString()
    }
    /**
     * Parse the NDEF message contents and print these to the on-screen log.
     */
    private fun processNdefMessages(ndefMessages: Array<NdefMessage?>) {
        // Go through all NDEF messages found on the NFC tag
        for (curMsg in ndefMessages) {
            if (curMsg != null) {
                // Print generic information about the NDEF message
                logMessage("Message", curMsg.toString())
                // The NDEF message usually contains 1+ records - print the number of recoreds
                logMessage("Records", curMsg.records.size.toString())

                // Loop through all the records contained in the message
                for (curRecord in curMsg.records) {
                    if (curRecord.toUri() != null) {
                        // URI NDEF Tag
                        logMessage("- URI", curRecord.toUri().toString())
                    } else {
                        // Other NDEF Tags - simply print the payload
                        logMessage("- Contents", curRecord.payload!!.contentToString())
                    }
                }
            }
        }
    }

    // --------------------------------------------------------------------------------
    // Utility functions

    /**
     * Save contents of the text view to the state. Ensures the text view contents survive
     * screen rotation.
     */
    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putCharSequence(KEY_LOG_TEXT, tv_messages.text)
        super.onSaveInstanceState(outState)
    }

    /**
     * Log a message to the debug text view.
     * @param header title text of the message, printed in bold
     * @param text optional parameter containing details about the message. Printed in plain text.
     */
    private fun logMessage(header: String, text: String?) {
        tv_messages.append(if (text.isNullOrBlank()) fromHtml("<b>$header</b><br>") else fromHtml("<b>$header</b>: $text<br>"))
        scrollDown()
    }

    /**
     * Convert HTML formatted strings to spanned (styled) text, for inserting to the TextView.
     * Externalized into an own function as the fromHtml(html) method was deprecated with
     * Android N. This method chooses the right variant depending on the OS.
     * @param html HTML-formatted string to convert to a Spanned text.
     */
    private fun fromHtml(html: String): Spanned {
        return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
    }

    /**
     * Scroll the ScrollView to the bottom, so that the latest appended messages are visible.
     */
    private fun scrollDown() {
        sv_messages.post({ sv_messages.smoothScrollTo(0, sv_messages.bottom) })
    }
}
