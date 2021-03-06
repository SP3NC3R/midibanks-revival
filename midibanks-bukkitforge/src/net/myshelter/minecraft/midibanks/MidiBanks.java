package net.myshelter.minecraft.midibanks;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EmptyStackException;
import java.util.HashSet;
import java.util.Stack;
import java.util.Timer;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
public class MidiBanks extends JavaPlugin implements Listener {
	boolean legacyBlockFace = BlockFace.NORTH.getModX() == -1;
	protected Timer player;
	protected ArrayList<SongInstance> songs;
	protected static final Logger log = Logger.getLogger("Minecraft");
	public static final int tempo = 20;
	boolean disallowAutostart = false;
	boolean disallowLoop = false;
	boolean redstone = true;
	public OutputPinHandler pinHandler;
	boolean novault = false;
	boolean hasperms = false;
	boolean opmode = false;
	boolean noperms = false;

	protected static void dolog(String msg) {
		MidiBanks.log.info("[MidiBanks] " + msg);
	}

	public boolean Allowed(String Permissionstr, Player player) {
		if (noperms = true) {
			hasperms = true;
		}
		if (opmode == true) {
			hasperms = player.isOp();
		}
		if ((opmode == false) && (noperms == false)) {

			hasperms = player.hasPermission(Permissionstr);
		}
		return hasperms;
	}

	///EVENT
	// AREA
	// //Player Event
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {

		if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			if (event.getClickedBlock().getType() != Material.WALL_SIGN) {
				return;
			}
			Sign midiSign = (Sign) event.getClickedBlock().getState();
			if (!midiSign.getLine(1).equalsIgnoreCase("[MIDI]")) {
				return;
			}
			try {

				if (!Allowed("midibanks.can-use", event.getPlayer())) {
					return;
				}
			} catch (NoClassDefFoundError e) {
			}
			SongInstance rc = null;
			for (int i = 0; i < songs.size(); i++) {
				if ((songs.get(i)).midiSign.getBlock().getLocation()
						.equals(midiSign.getBlock().getLocation())) {
					rc = songs.get(i);
					rc.toggle();
				}
			}
			if (rc == null) {
				learnMusic(midiSign);
			}
		}
		if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
			if (event.getClickedBlock().getType() != Material.WALL_SIGN) {
				return;
			}
			Sign midiSign = (Sign) event.getClickedBlock().getState();
			if (!midiSign.getLine(1).equalsIgnoreCase("[MIDI]")) {
				return;
			}
			try {
				if (Allowed("midibanks.can-use", event.getPlayer())) {
					return;
				}
			} catch (NoClassDefFoundError e) {
			}
			stopMusic(midiSign);
		}
	}

	// / Sign Event
	@EventHandler
	public void onSignChange(SignChangeEvent event) {
		if (!event.getLine(1).equalsIgnoreCase("[MIDI]")) {
			return;
		}
		try {
			if (Allowed("midibanks.can-create", event.getPlayer())) {
				return;
			}
		} catch (NoClassDefFoundError e) {
		}
		event.getBlock().setType(Material.AIR);
		event.getPlayer()
				.getWorld()
				.dropItemNaturally(event.getBlock().getLocation(),
						new ItemStack(Material.SIGN));
	}

	// //Chunk Load Event
	@EventHandler
	public void onChunkLoaded(ChunkLoadEvent event) {
		if (disallowAutostart) {
			return;
		}
		for (BlockState cbs : event.getChunk().getTileEntities()) {
			if (cbs.getBlock().getType() == Material.WALL_SIGN) {
				Sign midiSign = (Sign) cbs;
				if ((!midiSign.getLine(1).equalsIgnoreCase("[MIDI]"))
						|| (!midiSign.getLine(3).contains("A"))) {
					continue;
				}
				learnMusic(midiSign);
			}
		}
	}

	// //Redstone Event
	@EventHandler
	public void onBlockRedstoneChange(BlockRedstoneEvent event) {
		if (event.getBlock().getType() != Material.REDSTONE_WIRE) {
			return;
		}
		if (!redstone) {
			return;
		}
		boolean disableredstone = false;
		if ((event.getOldCurrent() == 0) || (event.getNewCurrent() != 0)) {
			disableredstone = false;
		} else if ((event.getOldCurrent() != 0) || (event.getNewCurrent() == 0)) {
			disableredstone = true;
		} else {
			return;
		}
		ArrayList<Block> checkSigns = new ArrayList<Block>();
		if ((event.getBlock().getRelative(1, 0, 0).getType() == Material.WALL_SIGN)
				&& (((org.bukkit.material.Sign) event.getBlock()
						.getRelative(1, 0, 0).getState().getData()).getFacing() == BlockFace.NORTH)) {
			checkSigns.add(event.getBlock().getRelative(1, 0, 0));
		}
		if ((event.getBlock().getRelative(-1, 0, 0).getType() == Material.WALL_SIGN)
				&& (((org.bukkit.material.Sign) event.getBlock()
						.getRelative(-1, 0, 0).getState().getData())
						.getFacing() == BlockFace.SOUTH)) {
			checkSigns.add(event.getBlock().getRelative(-1, 0, 0));
		}
		if ((event.getBlock().getRelative(0, 0, 1).getType() == Material.WALL_SIGN)
				&& (((org.bukkit.material.Sign) event.getBlock()
						.getRelative(0, 0, 1).getState().getData()).getFacing() == BlockFace.EAST)) {
			checkSigns.add(event.getBlock().getRelative(0, 0, 1));
		}
		if ((event.getBlock().getRelative(0, 0, -1).getType() == Material.WALL_SIGN)
				&& (((org.bukkit.material.Sign) event.getBlock()
						.getRelative(0, 0, -1).getState().getData())
						.getFacing() == BlockFace.WEST)) {
			checkSigns.add(event.getBlock().getRelative(0, 0, -1));
		}
		if (event.getBlock().getRelative(0, 1, 0).getType() == Material.WALL_SIGN) {
			checkSigns.add(event.getBlock().getRelative(0, 1, 0));
		}
		for (Block cb : checkSigns) {
			org.bukkit.block.Sign midiSign = (org.bukkit.block.Sign) cb
					.getState();
			if (midiSign.getLine(1).equalsIgnoreCase("[MIDI]")) {
				if (midiSign.getLine(3).contains("Y")) {
					if (!disableredstone) {
						boolean playing = false;
						for (int i = 0; i < songs.size(); i++) {
							if ((songs.get(i)).midiSign.getBlock()
									.getLocation()
									.equals(midiSign.getBlock().getLocation())) {
								playing = true;
								break;
							}
						}
						if (playing) {
							stopMusic(midiSign);
						} else {
							learnMusic(midiSign, true);
						}
					}
				} else if (disableredstone) {
					stopMusic(midiSign);
				} else {
					learnMusic(midiSign, true);
				}
			}
		}
		if (disableredstone) {
			return;
		}
		checkSigns = new ArrayList<Block>();
		if ((event.getBlock().getRelative(1, 0, 0).getType() == Material.WALL_SIGN)
				&& (((org.bukkit.material.Sign) event.getBlock()
						.getRelative(1, 0, 0).getState().getData()).getFacing() != BlockFace.NORTH)) {
			checkSigns.add(event.getBlock().getRelative(1, 0, 0));
		}
		if ((event.getBlock().getRelative(-1, 0, 0).getType() == Material.WALL_SIGN)
				&& (((org.bukkit.material.Sign) event.getBlock()
						.getRelative(-1, 0, 0).getState().getData())
						.getFacing() != BlockFace.SOUTH)) {
			checkSigns.add(event.getBlock().getRelative(-1, 0, 0));
		}
		if ((event.getBlock().getRelative(0, 0, 1).getType() == Material.WALL_SIGN)
				&& (((org.bukkit.material.Sign) event.getBlock()
						.getRelative(0, 0, 1).getState().getData()).getFacing() != BlockFace.EAST)) {
			checkSigns.add(event.getBlock().getRelative(0, 0, 1));
		}
		if ((event.getBlock().getRelative(0, 0, -1).getType() == Material.WALL_SIGN)
				&& (((org.bukkit.material.Sign) event.getBlock()
						.getRelative(0, 0, -1).getState().getData())
						.getFacing() != BlockFace.WEST)) {
			checkSigns.add(event.getBlock().getRelative(0, 0, -1));
		}
		for (Block cb : checkSigns) {
			org.bukkit.block.Sign midiSign = (org.bukkit.block.Sign) cb
					.getState();
			if (midiSign.getLine(1).equalsIgnoreCase("[MIDI]")) {
				SongInstance rc = null;
				for (int i = 0; i < songs.size(); i++) {
					if ((songs.get(i)).midiSign.getBlock().getLocation()
							.equals(midiSign.getBlock().getLocation())) {
						rc = songs.get(i);
						rc.toggle();
					}
				}
			}
		}
	}

	// //Chunk unload Event
	@EventHandler
	public void onChunkUnLoaded(ChunkUnloadEvent event) {
		for (BlockState cbs : event.getChunk().getTileEntities()) {
			if (cbs.getBlock().getType() == Material.WALL_SIGN) {
				Sign midiSign = (Sign) cbs;
				if (midiSign.getLine(1).equalsIgnoreCase("[MIDI]")) {
					stopMusic(midiSign);
				}
			}
		}
	}

	// /////////////////////EVENT AREA/////////////////////////////////
	@Override
	public void onEnable() {
		saveDefaultConfig();
		if (!getDataFolder().exists()) {
			getDataFolder().mkdir();
		}

		songs = new ArrayList<SongInstance>();
		disallowAutostart = getConfig().getBoolean("disallow-autostart", false);
		disallowLoop = getConfig().getBoolean("disallow-loop", false);
		redstone = getConfig().getBoolean("redstone", true);
		pinHandler = new MidiBanksOutputPinHandler(redstone);
		opmode = getConfig().getBoolean("opmode", false);
		noperms = getConfig().getBoolean("noperms", false);

		resetPlayer();
		getServer().getPluginManager().registerEvents(this, this);
		MidiBanks.dolog("Enabled! Version is " + getDescription().getVersion());

		if (!disallowAutostart) {
		MidiBanks.dolog("Auto-starting A banks in currently loaded chunks...");
		int count = 0;
		for (Object worldlist : getServer().getWorlds()) {
			for (Chunk loadedChunkslist : ((World) worldlist).getLoadedChunks()) {
				for (BlockState cbs : loadedChunkslist.getTileEntities()) {
					if (cbs.getBlock().getType() == Material.WALL_SIGN) {
						org.bukkit.block.Sign midiSign = (org.bukkit.block.Sign) cbs;
						if ((!midiSign.getLine(1).equalsIgnoreCase("[MIDI]"))
								|| (!midiSign.getLine(3).contains("A"))) {
							continue;
						}
						learnMusic(midiSign);
						count++;
					}
				}
			}
		}
		MidiBanks.dolog("Done; found " + count + " A banks.");
	}
	}

	@Override
	public void onDisable() {
		player.cancel();
		MidiBanks.log.info(String.format("[%s] Disabled Version %s",
				getDescription().getName(), getDescription().getVersion()));
	}

	public void resetPlayer() {
		songs.clear();
		player = new Timer();
		MidiPlayerStep np = new MidiPlayerStep(this);
		player.schedule(np, 20L, 20L);
	}

	public File getMidiFile(String name) {
		File midiFile = new File(getDataFolder() + "/" + name + ".mid");
		if (!midiFile.exists()) {
			Stack<File> dirs = new Stack<File>();
			dirs.push(getDataFolder());
			try {
				while (true) {
					File thisdir = dirs.pop();
					midiFile = new File(thisdir + "/" + name + ".mid");
					if (midiFile.exists()) {
						break;
					}
					if (thisdir.listFiles() != null) {
						for (File dircontent : thisdir.listFiles()) {
							if (dircontent.isDirectory()) {
								dirs.push(dircontent);
							}
						}
					}
				}
			} catch (EmptyStackException localEmptyStackException) {
			} catch (NullPointerException localNullPointerException) {
			}
		}
		if (midiFile.exists()) {
			return midiFile;
		}
		return null;
	}

	public void learnMusic(org.bukkit.block.Sign midiSign) {
		learnMusic(midiSign, false);
	}

	protected void learnMusic(org.bukkit.block.Sign midiSign, boolean fromRS) {
		if (!midiSign.getLine(1).equalsIgnoreCase("[MIDI]")) {
			return;
		}
		stopMusic(midiSign);
		int track = -1;
		int window = -1;
		int instrument = -1;
		double tempoCoef = 1.0D;
		String chans = "l";
		boolean chanCollapse = false;
		boolean shift = false;
		boolean loop = false;
		boolean display = false;
		boolean remrep = false;
		boolean repOctave = false;

		ArrayList<Block> checkRedstone = new ArrayList<Block>();
		if(legacyBlockFace) {
		if (((org.bukkit.material.Sign) midiSign.getData()).getFacing() == BlockFace.NORTH) {
			checkRedstone.add(midiSign.getBlock().getRelative(-1, 0, 0));
		}
		if (((org.bukkit.material.Sign) midiSign.getData()).getFacing() == BlockFace.SOUTH) {
			checkRedstone.add(midiSign.getBlock().getRelative(1, 0, 0));
		}
		if (((org.bukkit.material.Sign) midiSign.getData()).getFacing() == BlockFace.EAST) {
			checkRedstone.add(midiSign.getBlock().getRelative(0, 0, -1));
		}
		if (((org.bukkit.material.Sign) midiSign.getData()).getFacing() == BlockFace.WEST) {
			checkRedstone.add(midiSign.getBlock().getRelative(0, 0, 1));
		}
		}
		if(!legacyBlockFace) {
			if (((org.bukkit.material.Sign) midiSign.getData()).getFacing() == BlockFace.NORTH) {
				checkRedstone.add(midiSign.getBlock().getRelative(0, 0, -1));
			}
			if (((org.bukkit.material.Sign) midiSign.getData()).getFacing() == BlockFace.SOUTH) {
				checkRedstone.add(midiSign.getBlock().getRelative(0, 0, 1));
			}
			if (((org.bukkit.material.Sign) midiSign.getData()).getFacing() == BlockFace.EAST) {
				checkRedstone.add(midiSign.getBlock().getRelative(-1, 0, 0));
			}
			if (((org.bukkit.material.Sign) midiSign.getData()).getFacing() == BlockFace.WEST) {
				checkRedstone.add(midiSign.getBlock().getRelative(1, 0, 0));
			}
		}
		checkRedstone.add(midiSign.getBlock().getRelative(0, -1, 0));
		boolean hasRedstone = false;
		boolean powered = false;
		for (Block prb : checkRedstone) {
			if (prb.getType() == Material.REDSTONE_WIRE) {
				hasRedstone = true;
				if (prb.getData() <= 0) {
					continue;
				}
				powered = true;
			}
		}
		if ((redstone) && (!fromRS) && (hasRedstone) && (!powered)
				&& (!midiSign.getLine(3).contains("Y"))) {
			return;
		}

		Pattern pFileName = Pattern.compile("^[A-Za-z0-9_-]+$");
		Matcher mFileName = pFileName.matcher(midiSign.getLine(2));
		if (mFileName.find()) {
			try {
				File midiFile = getMidiFile(midiSign.getLine(2));
				if (midiFile == null) {
					return;
				}

				String settings = midiSign.getLine(3);
				Pattern pNextSign = Pattern.compile("N([lrud])");
				Matcher mNextSign = pNextSign.matcher(midiSign.getLine(3));
				if (mNextSign.find()) {
					BlockFace direction = ((org.bukkit.material.Sign) midiSign
							.getData()).getFacing();
					Block SelectedBlock = null;
					int sx = 0;
					int sz = 0;
					if (legacyBlockFace){
					if (direction == BlockFace.NORTH) {
						sx = -1;
					}
					if (direction == BlockFace.SOUTH) {
						sx = 1;
					}
					if (direction == BlockFace.EAST) {
						sz = 1;
					}
					if (direction == BlockFace.WEST) {
						sz = -1;
					}
					}
					if (!legacyBlockFace){
						if (direction == BlockFace.NORTH) {
							sz = -1;
						}
						if (direction == BlockFace.SOUTH) {
							sz = 1;
						}
						if (direction == BlockFace.EAST) {
							sx = 1;
						}
						if (direction == BlockFace.WEST) {
							sx = -1;
						}
					}
					if (mNextSign.group(1).equals("l")) {
						SelectedBlock = midiSign.getBlock().getRelative(sx, 0,
								sz);
					}
					if (mNextSign.group(1).equals("r")) {
						SelectedBlock = midiSign.getBlock().getRelative(
								-1 * sx, 0, -1 * sz);
					}
					if (mNextSign.group(1).equals("u")) {
						SelectedBlock = midiSign.getBlock()
								.getRelative(0, 1, 0);
					}
					if (mNextSign.group(1).equals("d")) {
						SelectedBlock = midiSign.getBlock().getRelative(0, -1,
								0);
					}
					if ((SelectedBlock.getType() == Material.WALL_SIGN)
							|| (SelectedBlock.getType() == Material.SIGN_POST)) {
						org.bukkit.block.Sign setSign = (org.bukkit.block.Sign) SelectedBlock
								.getState();
						settings = "";
						for (int i = 0; i < 4; i++) {
							settings = settings + setSign.getLine(i);
						}
					}
				}

				Pattern pTrack = Pattern.compile("T([0-9a-f]+)");
				Matcher mTrack = pTrack.matcher(settings);
				if (mTrack.find()) {
					track = Integer.parseInt(mTrack.group(1), 16);
				}

				Pattern pChans = Pattern
						.compile("=([0123456789abcdeflmnosz ]+)");
				Matcher mChans = pChans.matcher(settings);
				if (mChans.find()) {
					chans = mChans.group(1);
				}

				Pattern pTempo = Pattern.compile("(<|>)([2-9])");
				Matcher mTempo = pTempo.matcher(settings);
				if (mTempo.find()) {
					if (mTempo.group(1).equals("<")) {
						tempoCoef = 1.0D / Integer.parseInt(mTempo.group(2));
					}
					if (mTempo.group(1).equals(">")) {
						tempoCoef = Integer.parseInt(mTempo.group(2));
					}
				}
				Pattern pFineTempo = Pattern.compile("(\\{|\\})([1-9])");
				Matcher mFineTempo = pFineTempo.matcher(settings);
				if (mFineTempo.find()) {
					if (mFineTempo.group(1).equals("{")) {
						tempoCoef -= Integer.parseInt(mFineTempo.group(2)) / 10.0D;
					}
					if (mFineTempo.group(1).equals("}")) {
						tempoCoef += Integer.parseInt(mFineTempo.group(2)) / 10.0D;
					}
				}

				Pattern pInstrument = Pattern.compile("I([0-9])");
				Matcher mInstrument = pInstrument.matcher(settings);
				if (mInstrument.find()) {
					instrument = Integer.parseInt(mInstrument.group(1));
				}

				Pattern pWindow = Pattern.compile("W([0-9])");
				Matcher mWindow = pWindow.matcher(settings);
				if (mWindow.find()) {
					window = Integer.parseInt(mWindow.group(1));
				}

				if (settings.contains("C")) {
					chanCollapse = true;
				}
				if (settings.contains("S")) {
					shift = true;
				}
				if (settings.contains("L")) {
					loop = !disallowLoop;
				}
				if (settings.contains("D")) {
					display = true;
				}
				if (settings.contains("X")) {
					remrep = true;
				}
				if (settings.contains("R")) {
					repOctave = true;
				}

				Sequence midi = MidiSystem.getSequence(midiFile);
				if (midi.getTracks().length <= track) {
					return;
				}

				if (!settings.contains("O")) {
					int realTempo = 0;
					Track first = midi.getTracks()[0];
					for (int i = 0; i < first.size(); i++) {
						if ((first.get(i).getMessage().getStatus() == 255)
								&& (first.get(i).getMessage().getMessage()[1] == 81)) {
							byte[] bf = first.get(i).getMessage().getMessage();
							for (int j = 3; j < 6; j++) {
								realTempo <<= 8;
								realTempo += bf[j];
							}
							break;
						}
					}
					if (realTempo > 0) {
						tempoCoef *= (500000.0D / realTempo) * 0.8D;
					}

				}

				if (track < 0) {
					for (int i = 0; i < midi.getTracks().length; i++) {
						SongInstance SongInst = new SongInstance(this,
								midiSign, midi.getTracks()[i], chans);
						SongInst.track = i;
						SongInst.resolution = Math
								.floor(midi.getResolution() / 24);
						SongInst.chanCollapse = chanCollapse;
						SongInst.shift = shift;
						SongInst.loop = loop;
						SongInst.display = display;
						SongInst.tempoCoef = tempoCoef;
						SongInst.remRepeated = remrep;
						SongInst.window = window;
						SongInst.repOctave = repOctave;
						SongInst.instrument = Integer.valueOf(instrument);
						songs.add(SongInst);
					}
				} else {
					SongInstance SongInst = new SongInstance(this, midiSign,
							midi.getTracks()[track], chans);
					SongInst.track = track;
					SongInst.resolution = Math.floor(midi.getResolution() / 24);
					SongInst.chanCollapse = chanCollapse;
					SongInst.shift = shift;
					SongInst.loop = loop;
					SongInst.display = display;
					SongInst.tempoCoef = tempoCoef;
					SongInst.remRepeated = remrep;
					SongInst.window = window;
					SongInst.repOctave = repOctave;
					SongInst.instrument = Integer.valueOf(instrument);
					songs.add(SongInst);
				}
				midiSign.setLine(0, "PLAYING");
			} catch (InvalidMidiDataException imde) {
				midiSign.setLine(0, "NOT A MIDI");
			} catch (IOException ioe) {
				midiSign.setLine(0, "CAN'T READ FILE");
			}
		} else {
			midiSign.setLine(0, "BAD FILENAME");
		}
		getServer().getScheduler().scheduleSyncDelayedTask(this,
				new UpdateSign(midiSign));
	}

	public void stopMusic(org.bukkit.block.Sign midiSign) {
		try {
			for (int i = 0; i < songs.size(); i++) {
				if (midiSign
						.getBlock()
						.getLocation()
						.equals((songs.get(i)).midiSign.getBlock()
								.getLocation())) {
					songs.remove(i);
					i--;
				}
			}
			midiSign.setLine(0, "");
			getServer().getScheduler().scheduleSyncDelayedTask(this,
					new UpdateSign(midiSign));
		} catch (NullPointerException localNullPointerException) {
		}
	}

	// Command area
	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		if (!command.getName().equalsIgnoreCase("midibanks")) {
			return false;
		}

		if (args.length < 1) {
			return true;
		}

		boolean admin = false;
		Player player = (Player) sender;
		try {
			if ((sender instanceof ConsoleCommandSender)
					|| Allowed("midibanks.cmd", player)) {
				admin = true;
			}
		} catch (NoClassDefFoundError e) {

		}
		if ((args[0].equalsIgnoreCase("halt")) & (admin)) {
			this.player.cancel();
			resetPlayer();
		}
		if ((args[0].equalsIgnoreCase("saveconfig")) & (admin)) {
			saveConfig();
		}
		if ((args[0].equalsIgnoreCase("reloadconfig")) & (admin)) {
			reloadConfig();
		}

		String bychan;
		int i;
		// play-song <filename>
		if ((args[0].equalsIgnoreCase("playsong")) & (args.length >= 2)
				& (admin = true)) {
			Pattern pFileName = Pattern.compile("^[A-Za-z0-9_-]+$");
			Matcher mFileName = pFileName.matcher(args[1]);
			if (mFileName.find()) {

			}
		}
		// channels <filename>
		if ((args[0].equalsIgnoreCase("channels")) & (args.length >= 2)
				& (admin == true)) {
			Pattern pFileName = Pattern.compile("^[A-Za-z0-9_-]+$");
			Matcher mFileName = pFileName.matcher(args[1]);
			if (mFileName.find()) {
				try {
					File midiFile = getMidiFile(args[1]);
					if (midiFile == null) {
						return true;
					}
					Sequence midi = MidiSystem.getSequence(midiFile);
					sender.sendMessage("== MIDI Sequence " + args[1]
							+ ".mid - Channels ==");
					boolean[] Channels = new boolean[16];
					for (int numberofchannels = 0; numberofchannels < Channels.length; numberofchannels++) {
						Channels[numberofchannels] = false;
					}
					for (Track Tracks : midi.getTracks()) {
						for (int numoftracks = 0; numoftracks < Tracks.size(); numoftracks++) {
							if ((Tracks.get(numoftracks).getMessage()
									.getStatus() >> 4) == 9) {
								Channels[(Tracks.get(numoftracks).getMessage()
										.getStatus() & 0xF)] = true;
							}
						}
					}
					bychan = "";
					for (i = 0; i < Channels.length; i++) {
						if (Channels[i] != false) {
							bychan = bychan + Integer.toHexString(i) + " ";
						}
					}
					sender.sendMessage("Used: " + bychan);
				} catch (InvalidMidiDataException imde) {
					sender.sendMessage("Error reading MIDI data. Is this a MIDI file?");
				} catch (IOException ioe) {
					sender.sendMessage("No such file!");
				}
			} else {
				sender.sendMessage("Invalid filename. Filenames can only have letters, numbers, underscores and dashes.");
			}
		}
		// list command
		if ((args[0].equalsIgnoreCase("list")) & (admin == true)) {
			String result = "";
			File[] Files;
			HashSet<String> names = new HashSet<String>();
			Stack<File> dirs = new Stack<File>();
			dirs.push(getDataFolder());
			try {
				while (true) {
					File thisdir = dirs.pop();
					i = (Files = thisdir.listFiles()).length;
					for (File as : Files) {
						File dircontent = as;
						if (dircontent.isDirectory()) {
							dirs.push(dircontent);
						} else if (dircontent.getName().endsWith(".mid")) {
							names.add(dircontent.getName().substring(0,
									dircontent.getName().length() - 4));
						}
					}
				}
			} catch (EmptyStackException localEmptyStackException) {
			} catch (NullPointerException localNullPointerException) {
			}
			ArrayList<String> sortNames = new ArrayList<String>();
			sortNames.addAll(names);
			Collections.sort(sortNames);
			int page = 0;
			int maxpage = (int) Math.floor(sortNames.size() / 40);
			try {
				if (args.length > 1) {
					page = Integer.parseInt(args[1]) - 1;
				}
			} catch (NumberFormatException localNumberFormatException) {
			}
			if (page > maxpage) {
				page = maxpage;
			}
			if (page < 0) {
				page = 0;
			}
			sender.sendMessage("== List of available MIDI files == (page "
					+ (page + 1) + " of " + (maxpage + 1) + ")");
			for (int i1 = page * 40; (i1 < ((page + 1) * 40))
					&& (i1 < sortNames.size());) {
				result = "";
				for (int j = 0; (j < 10) && (i1 < sortNames.size()); i1++) {
					result = result + sortNames.get(i1) + " ";

					j++;
				}
				if (result == "") {
					continue;
				}
				sender.sendMessage(result);
			}
		}
		return true;
	}

	class UpdateSign implements Runnable {
		private org.bukkit.block.Sign midiSign;

		UpdateSign(org.bukkit.block.Sign midiSign) {
			this.midiSign = midiSign;
		}

		@Override
		public void run() {
			midiSign.update();
		}
	}
}