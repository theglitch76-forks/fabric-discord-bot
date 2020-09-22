package net.fabricmc.bot.config

import com.gitlab.kordlib.common.entity.Snowflake
import com.gitlab.kordlib.core.entity.Guild
import com.gitlab.kordlib.core.entity.Role
import com.gitlab.kordlib.core.entity.channel.Channel
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.Feature
import com.uchuhimo.konf.source.toml
import net.fabricmc.bot.MissingChannelException
import net.fabricmc.bot.MissingGuildException
import net.fabricmc.bot.MissingRoleException
import net.fabricmc.bot.bot
import net.fabricmc.bot.config.spec.BotSpec
import net.fabricmc.bot.config.spec.ChannelsSpec
import net.fabricmc.bot.config.spec.RolesSpec
import net.fabricmc.bot.enums.Channels
import net.fabricmc.bot.enums.Roles
import java.io.File

/**
 * Central object representing this bot's configuration, wrapping a Konf [FabricBotConfig] object.
 *
 * Config data is loaded from four locations, with values from earlier locations being
 * overridden by values in later locations:
 *
 * * The file `default.toml`, which is bundled with the bot as a resource.
 * * The file `config.toml`, located in the current working directory. If you edit this file
 *   at runtime, then it will automatically be reloaded.
 * * Environment variables defined at runtime.
 * * System properties defined at runtime.
 *
 * The currently-loaded configuration is always available at the [config] property.
 */
class FabricBotConfig {
    private var config = Config { addSpec(BotSpec); addSpec(ChannelsSpec); addSpec(RolesSpec) }
            .from.enabled(Feature.FAIL_ON_UNKNOWN_PATH).toml.resource("default.toml")
            .from.env()
            .from.systemProperties()

    init {
        if (File("config.toml").exists()) {
            config = config.from.toml.watchFile("config.toml")
        }
    }

    /**
     * The bot's login token.
     */
    val token: String get() = config[BotSpec.token]

    /**
     * The bot's command prefix.
     */
    val prefix: String get() = config[BotSpec.commandPrefix]

    /**
     * The [Snowflake] object representing the bot's configured primary guild.
     */
    val guildSnowflake: Snowflake get() = Snowflake(config[BotSpec.guild])

    /**
     * Given a [Channels] enum value, attempt to retrieve the corresponding Discord [Channel]
     * object.
     *
     * @param channel The corresponding [Channels] enum value to retrieve the channel for.
     * @return The [Channel] object represented by the given [Channels] enum value.
     * @throws MissingChannelException Thrown if the configured [Channel] cannot be found.
     */
    @Throws(MissingChannelException::class)
    suspend fun getChannel(channel: Channels): Channel {
        val snowflake = when (channel) {
            Channels.ACTION_LOG    -> Snowflake(config[ChannelsSpec.actionLog])
            Channels.BOT_COMMANDS  -> Snowflake(config[ChannelsSpec.botCommands])
            Channels.MODERATOR_LOG -> Snowflake(config[ChannelsSpec.moderatorLog])
        }

        return bot.kord.getChannel(snowflake) ?: throw MissingChannelException(snowflake.longValue)
    }

    /**
     * Given a [Roles] enum value, retrieve a [Snowflake] corresponding with a configured role.
     *
     * @param role The corresponding [Roles] enum value to retrieve the [Snowflake] for.
     * @return The [Snowflake] object represented by the given [Roles] enum value.
     */
    fun getRoleSnowflake(role: Roles): Snowflake {
        return when (role) {
            Roles.ADMIN     -> Snowflake(config[RolesSpec.admin])
            Roles.MODERATOR -> Snowflake(config[RolesSpec.mod])
            Roles.MUTED     -> Snowflake(config[RolesSpec.muted])
            Roles.OWNER     -> Snowflake(config[RolesSpec.owner])
        }
    }

    /**
     * Given a [Roles] enum value, attempt to retrieve the corresponding Discord [Role]
     * object.
     *
     * @param role The corresponding [Roles] enum value to retrieve the channel for.
     * @return The [Role] object represented by the given [Roles] enum value.
     * @throws MissingRoleException Thrown if the configured [Role] cannot be found.
     */
    @Throws(MissingRoleException::class)
    suspend fun getRole(role: Roles): Role {
        val snowflake = getRoleSnowflake(role)

        return getGuild().getRoleOrNull(snowflake) ?: throw MissingRoleException(snowflake.longValue)
    }

    /**
     * Attempt to retrieve the [Guild] object for the configured primary guild.
     *
     * @return The [Guild] object representing the configured primary guild.
     * @throws MissingGuildException Thrown if the configured [Guild] cannot be found.
     */
    @Throws(MissingGuildException::class)
    suspend fun getGuild(): Guild =
            bot.kord.getGuild(guildSnowflake) ?: throw MissingGuildException(guildSnowflake.longValue)
}
/**
 * The currently loaded [FabricBotConfig].
 *
 * You should always use this instead of constructing an instance of [FabricBotConfig] yourself.
 */
val config = FabricBotConfig()