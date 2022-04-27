package com.github

import com.github.commands.YanCommands
import com.github.core.Paints
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.liuwj.ktorm.entity.add
import me.liuwj.ktorm.entity.toList
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.permission.PermissionId
import net.mamoe.mirai.console.permission.PermissionService
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.ListeningStatus
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.globalEventChannel
import net.mamoe.mirai.message.code.MiraiCode.deserializeMiraiCode
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.toMessageChain
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import java.io.ByteArrayOutputStream
import java.net.URL
import java.util.concurrent.ThreadLocalRandom
import javax.imageio.ImageIO
import kotlin.random.asKotlinRandom

object XXYan : KotlinPlugin(JvmPluginDescription(
    id = "com.github.XXYan",
    name = "XXYan",
    version = "0.0.2",
) {
    author("gsycl2004")
    info("""to record someone's word""")
}) {
    val permission by lazy {
        PermissionService.INSTANCE.register(PermissionId("yan", "command"), "yans")
    }

    fun String.judgeRegex(name: String): Boolean {
        val regex = "^$name *(.*)".toRegex()
        return regex.containsMatchIn(this)
    }

    fun String.matchList(name: String): List<String> {
        val regex = "($name) *(.*)".toRegex()
        return regex.find(this)!!.groupValues.toList()
    }

    override fun onEnable() {
        YanCommands.register()
        Class.forName("org.sqlite.JDBC")
        YanCommand.register()
        YanConfig.reload()
        globalEventChannel().subscribe<GroupMessageEvent> {
            if (sender.id in YanConfig.cares.values && this.message.contentToString() != "") {
                var yan = YanData.getSequence(sender.id)
                yan.add(YanEntity {
                    name = senderName
                    head = sender.avatarUrl
                    this.yan = message.serializeToMiraiCode()
                })
            }
            return@subscribe ListeningStatus.LISTENING
        }
        globalEventChannel().subscribe<GroupMessageEvent> {
            if (YanConfig.cares.keys.firstOrNull { this.message.contentToString().judgeRegex(it) } != null) {
                val args = this.message.contentToString()
                    .matchList(YanConfig.cares.keys.first { this.message.contentToString().judgeRegex(it) })
                println(args)
                val pc = YanData.getSequence(YanConfig.cares[args[1]]!!)
                val yan: YanEntity = if (args.size < 3) {

                    pc.toList().random(ThreadLocalRandom.current().asKotlinRandom())
                } else pc.toList().filter {
                    it.yan.lowercase().contains(args[2].lowercase())
                }.also { println(it) }.randomOrNull(ThreadLocalRandom.current().asKotlinRandom()) ?: YanEntity {
                    this.name = sender.nameCardOrNick
                    this.head = sender.avatarUrl
                    this.yan = "貌似他没说过这句话呢。"
                }
                val head = withContext(Dispatchers.IO) {
                    ImageIO.read(withContext(Dispatchers.IO) {
                        URL(yan.head).openStream()
                    })

                }
                val image = Paints.paintTextMessage(
                    head,
                    yan.name,
                    yan.yan.deserializeMiraiCode().filterIsInstance<PlainText>().joinToString("") { it.content })
                val byteStream = ByteArrayOutputStream()
                ImageIO.write(image, "png", byteStream)
                val miraiImage = group.uploadImage(byteStream.toByteArray().toExternalResource("png"))
                this.group.sendMessage(miraiImage)
            }
            return@subscribe ListeningStatus.LISTENING
        }


    }
    //globalEventChannel().subscribe<GroupMessageEvent> {
    //    if (message.contentToString().startsWith("test")){
    //        val args = message.contentToString().split(" ")
    //        val head = ImageIO.read(withContext(Dispatchers.IO) {
    //            URL(sender.avatarUrl).openStream()
    //        })
    //        val image = Paints.paintTextMessage(head,senderName,args[1])
    //        val byteStream = ByteArrayOutputStream()
    //        ImageIO.write(image,"png",byteStream)
    //        val miraiImage = group.uploadImage(byteStream.toByteArray().toExternalResource("png"))
    //        this.group.sendMessage(miraiImage)
    //    }
    //    return@subscribe ListeningStatus.LISTENING
    //}

}