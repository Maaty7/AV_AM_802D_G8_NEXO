package cl.duoc.nexo.monitoring

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

object AppCategoryClassifier {

    private val mapaConocidos = mapOf(
        // Navegación (se agrupa dentro de Educación para el informe, según el criterio del proyecto)
        "com.android.chrome" to "Educación",
        "org.mozilla.firefox" to "Educación",
        "com.microsoft.emmx" to "Educación",
        "com.sec.android.app.sbrowser" to "Educación",
        "com.opera.browser" to "Educación",
        "com.brave.browser" to "Educación",
        "com.duckduckgo.mobile.android" to "Educación",

        // Comunicación
        "com.whatsapp" to "Comunicación",
        "com.whatsapp.w4b" to "Comunicación",
        "com.google.android.gm" to "Comunicación",
        "com.facebook.orca" to "Comunicación",
        "org.telegram.messenger" to "Comunicación",
        "com.google.android.apps.messaging" to "Comunicación",
        "com.samsung.android.messaging" to "Comunicación",
        "com.samsung.android.email.provider" to "Comunicación",
        "com.microsoft.office.outlook" to "Comunicación",
        "com.discord" to "Comunicación",
        "com.google.android.apps.tachyon" to "Comunicación",
        "com.google.android.apps.meetings" to "Comunicación",
        "us.zoom.videomeetings" to "Comunicación",
        "com.microsoft.teams" to "Comunicación",
        "com.skype.raider" to "Comunicación",
        "com.viber.voip" to "Comunicación",
        "jp.naver.line.android" to "Comunicación",
        "org.thoughtcrime.securesms" to "Comunicación",
        "com.google.android.dialer" to "Comunicación",
        "com.samsung.android.dialer" to "Comunicación",

        // Redes Sociales (separado de Entretenimiento: plataformas sociales, no streaming)
        "com.instagram.android" to "Redes Sociales",
        "com.zhiliaoapp.musically" to "Redes Sociales",
        "com.ss.android.ugc.trill" to "Redes Sociales",
        "com.twitter.android" to "Redes Sociales",
        "com.facebook.katana" to "Redes Sociales",
        "com.facebook.lite" to "Redes Sociales",
        "com.snapchat.android" to "Redes Sociales",
        "com.pinterest" to "Redes Sociales",
        "com.reddit.frontpage" to "Redes Sociales",

        // Entretenimiento (solo video/audio/streaming, sin redes sociales)
        "com.google.android.youtube" to "Entretenimiento",
        "com.google.android.apps.youtube.music" to "Entretenimiento",
        "com.netflix.mediaclient" to "Entretenimiento",
        "com.spotify.music" to "Entretenimiento",
        "com.disney.disneyplus" to "Entretenimiento",
        "com.wbd.stream" to "Entretenimiento",
        "com.amazon.avod.thirdpartyclient" to "Entretenimiento",
        "tv.twitch.android.app" to "Entretenimiento",
        "com.kwai.video" to "Entretenimiento",
        "com.soundcloud.android" to "Entretenimiento",
        "deezer.android.app" to "Entretenimiento",

        // Juegos
        "com.king.candycrushsaga" to "Juegos",
        "com.roblox.client" to "Juegos",
        "com.mojang.minecraftpe" to "Juegos",
        "com.supercell.clashofclans" to "Juegos",
        "com.supercell.clashroyale" to "Juegos",
        "com.supercell.brawlstars" to "Juegos",
        "com.dts.freefireth" to "Juegos",
        "com.dts.freefiremax" to "Juegos",
        "com.tencent.ig" to "Juegos",
        "com.activision.callofduty.shooter" to "Juegos",
        "com.miHoYo.GenshinImpact" to "Juegos",
        "com.innersloth.spacemafia" to "Juegos",
        "com.kiloo.subwaysurf" to "Juegos",
        "com.miniclip.eightballpool" to "Juegos",
        "com.ea.gp.fifamobile" to "Juegos",
        "com.rovio.angrybirds" to "Juegos",
        "com.gameloft.android.ANMP.GloftA9HM" to "Juegos",
        "com.imangi.templerun2" to "Juegos",
        "com.playrix.homescapes" to "Juegos",
        "com.zeptolab.ctr.ads" to "Juegos",

        // Educación
        "com.google.android.apps.classroom" to "Educación",
        "com.duolingo" to "Educación",
        "com.google.android.calculator" to "Educación",
        "com.android.calculator2" to "Educación",
        "com.sec.android.app.popupcalculator" to "Educación",
        "com.google.android.apps.docs" to "Educación",
        "com.google.android.apps.docs.editors.docs" to "Educación",
        "com.google.android.apps.docs.editors.sheets" to "Educación",
        "com.google.android.apps.docs.editors.slides" to "Educación",
        "com.google.android.keep" to "Educación",
        "com.samsung.android.app.notes" to "Educación",
        "com.google.android.apps.translate" to "Educación",
        "org.wikipedia" to "Educación",
        "com.microsoft.office.word" to "Educación",
        "com.microsoft.office.excel" to "Educación",
        "com.microsoft.office.powerpoint" to "Educación",
        "com.microsoft.office.officehubrow" to "Educación",
        "com.adobe.reader" to "Educación",
        "com.quizlet.quizletandroid" to "Educación",
        "org.kahoot.mobile.access" to "Educación",
        "com.microblink.photomath" to "Educación",
        "org.khanacademy.android" to "Educación",
        "com.instructure.candroid" to "Educación",
        "com.moodle.moodlemobile" to "Educación",
        "com.openai.chatgpt" to "Educación",
        "com.google.android.apps.bard" to "Educación",
        "com.anthropic.claude" to "Educación"
    )

    fun clasificar(packageName: String, context: Context): String {
        mapaConocidos[packageName]?.let { return it }

        return try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            when (appInfo.category) {
                ApplicationInfo.CATEGORY_GAME -> "Juegos"
                ApplicationInfo.CATEGORY_SOCIAL -> "Redes Sociales"
                ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Educación"
                ApplicationInfo.CATEGORY_VIDEO,
                ApplicationInfo.CATEGORY_AUDIO,
                ApplicationInfo.CATEGORY_IMAGE -> "Entretenimiento"
                ApplicationInfo.CATEGORY_NEWS -> "Otros"
                else -> "Otros"
            }
        } catch (e: PackageManager.NameNotFoundException) {
            "Otros"
        }
    }
}
