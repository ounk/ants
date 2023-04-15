package cool.ants.com

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import kotlinx.coroutines.*
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import java.io.InputStream
import java.net.InetAddress
import java.nio.charset.StandardCharsets
import java.util.*


class MainActivity : Activity() {
    //lateinit var videoView: VideoView
    private lateinit var mVideoLayout: VLCVideoLayout
    private lateinit var session: Session
    private lateinit var adress: String

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                adress = InetAddress.getByName("www.antstream.de").hostAddress
                //adress = InetAddress.getByName("ants.zapto.org").address.toString()
                //adress = InetAddress.getByName("192.168.178.30").hostAddress
                Log.v("adresse: ", adress.toString())

                val s = String(InetAddress.getByName("www.antstream.de").address, StandardCharsets.UTF_8)
                Log.v("adresse: ", s)

                session=sshConnect()
            }
        }

    }

    override fun onResume() {
        super.onResume()
        val args = ArrayList<String>()
        args.add("-vvv")
        val mLibVLC = LibVLC(this@MainActivity, args)
        val mMediaPlayer = MediaPlayer(mLibVLC)
        mVideoLayout = findViewById(R.id.video_layout)
        mMediaPlayer.attachViews(mVideoLayout, null, false, false)
        val abc = Media(mLibVLC, Uri.parse("rtsp://$adress:8554/mystream"))
        mMediaPlayer.media = abc
        abc.release()
        mMediaPlayer.play()

    }

    private fun sshConnect(): Session {
        val sess = JSch().getSession("android", adress, 22)
        sess.setPassword("ERH*r{7#jY-&")
        val prop = Properties()
        prop["StrictHostKeyChecking"] = "no"
        sess.setConfig(prop)
        sess.connect()
        return sess
    }

    private fun sshBefehl(befehl: String?): ArrayList<String> {
        // Erstelle ein neues Objekt, für einen neuen SSH Channel.
        val channelssh = session.openChannel("exec") as ChannelExec

        channelssh.setCommand(befehl)
        channelssh.connect()
        val buffer = ByteArray(1024)
        val lines = ArrayList<String>()
        // Fange an die Ausgaben der Konsole auszulesen.
        try {
            val `in`: InputStream = channelssh.inputStream
            var line = ""

            // Lese alle Ausgaben aus, bis der Befehl beendet wurde oder die Verbindung abbricht.
            while (true) {

                // Schreibe jede Zeile der Konsolenausgabe in unser Array.
                while (`in`.available() > 0) {
                    val i: Int = `in`.read(buffer, 0, 1024)

                    // Brich die Protokollierung der Ausgabe, für diese Zeile, ab, wenn die Ausgabe leer sein sollte.
                    if (i < 0) {
                        break
                    }
                    line = String(buffer, 0, i)
                    lines.add(line)
                }

                // Wir wurden ausgeloggt.
                if (line.contains("logout")) {
                    break
                }

                // Befehl beendet oder Verbindung abgebrochen.
                if (channelssh.isClosed) {
                    break
                }

                // Warte einen kleinen Augenblick mit der nächsten Zeile.
                try {
                    Thread.sleep(1000)
                } catch (ee: Exception) {
                }
            }
        } catch (e: Exception) {
        }
        channelssh.disconnect()

        return lines
    }

    fun onClick(view: View?) {
        //val videoView: VideoView = findViewById(R.id.rtspVideo)
        /*videoView.setVideoURI(Uri.parse("rtsp://ants.zapto.org:8554/mystream"))
        videoView.start()*/
        val stepBar: SeekBar = findViewById(R.id.seekBar)
        val stepSize = stepBar.progress

        var servo = 0
        var direction = 0

        var lightOn = -1

        when (view) {
            findViewById<Button>(R.id.butLeft) -> {
                servo = 0
                direction = stepSize
            }
            findViewById<Button>(R.id.butRight) -> {
                servo = 0
                direction = -stepSize
            }
            findViewById<Button>(R.id.butUp) -> {
                servo = 1
                direction = stepSize
            }
            findViewById<Button>(R.id.butDown) -> {
                servo = 1
                direction = -stepSize
            }
            findViewById<Button>(R.id.butFor) -> {
                servo = 2
                direction = stepSize
            }
            findViewById<Button>(R.id.butBack) -> {
                servo = 2
                direction = -stepSize
            }
            findViewById<Button>(R.id.butReset) -> {
                GlobalScope.launch {
                    withContext(Dispatchers.IO) {
                        sshBefehl("python3 /home/android/servo/scene.py 1")
                    }
                }

            }
            findViewById<Button>(R.id.butLightOn) -> {
                lightOn = 1
            }
            findViewById<Button>(R.id.butLightOff) -> {
                lightOn = 0
            }
        }

        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                if (lightOn > -1)
                    sshBefehl("python3 /home/android/hue/light.py $lightOn")
                else
                    sshBefehl("python3 /home/android/servo/move.py $servo $direction")
            }
        }
    }
}