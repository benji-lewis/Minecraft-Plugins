package uk.greenparty.managers;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import uk.greenparty.GreenPartyPlugin;

import java.util.*;

/**
 * Manages the Green Party NPCs — the Eternal Council of the Verdant Utopia.
 *
 * Each NPC represents a different faction within the Green Party:
 * - The Composters (moderates, believe change comes through soil management)
 * - The Protesters (radicals, believe change comes through standing in the road)
 * - The Bureaucrats (pragmatists, believe change comes through 47-point policy documents)
 * - The Enthusiasts (everyone else, believe change comes through enthusiasm alone)
 *
 * NPCs are spawned as Villagers with custom names and equipment.
 * They will follow you around occasionally and say things.
 * This is intentional. The Green Party never stops talking.
 *
 * Version 1.0.1: Expanded council from 6 to 13 members, scattered across the dimension.
 */
public class NpcManager {

    private final GreenPartyPlugin plugin;
    private final List<Entity> spawnedNpcs = new ArrayList<>();
    private final Random random = new Random();

    // Surface Y from VerdantChunkGenerator
    private static final int SURFACE_Y = 55;

    // The council members and their wisdom (expanded to 13 for v1.0.1, dialogue expanded to 15+ in v1.3.0)
    private static final NpcDefinition[] COUNCILLORS = {
        // === SPAWN AREA COUNCILLORS (within 16 blocks of spawn) ===
        new NpcDefinition(
            "§2Councillor Wheatgrass",
            Villager.Profession.FARMER,
            new String[]{
                "Ah, a new arrival! Have you signed our 14 active petitions?",
                "The composting rota is on the notice board. §8(There is no notice board.)",
                "I've been a Green Party member for 23 years. I've attended 847 meetings. We've passed 12 resolutions.",
                "Did you know dirt is sentient? Metaphorically? The council passed a motion on it.",
                "Every block you place here is a vote for the environment. Every block. Even gravel.",
                "We're planning a peaceful protest against cobblestone. It's too grey.",
                "I chair 6 sub-committees. Five of them are about the other one. It's recursive governance.",
                "The new manifesto has 94 points! I wrote 47 of them. Points 48-94 were also me. Different coloured ink.",
                "I once composted an agenda. The irony was not lost on me. I composted that irony too.",
                "We're considering making every 7th block legally designated as 'heritage terrain'. Paperwork in progress.",
                "The best part of being a councillor? The meetings. There are so many. I live there now.",
                "I voted yes on Motion 88c, 91a, 95b, 97c, 99d, 103f, and 107. I don't remember what any of them were.",
                "Someone brought a coal block to our last gala. §8They were escorted out. With forms.",
                "The council has approved a new colour for the Verdant Utopia: greener green. Pantone code TBD.",
                "Our composting output is up 12% this quarter! The council celebrated with a compost-themed banquet.",
            }
        ),
        new NpcDefinition(
            "§aRecycling Evangelist Bramble",
            Villager.Profession.LIBRARIAN,
            new String[]{
                "Did you know that 97.3% of this dimension is recyclable? I calculated it.",
                "I've recycled my breakfast 4 times. Today. Each recycling makes it healthier.",
                "You can compost almost anything. I once composted a complaint form.",
                "This dirt block? Recycled. This grass block? Recycled. §8My opinions? Also recycled.",
                "REDUCE! REUSE! RECYCLE! I put that on everything. Business cards, protest signs, cake.",
                "I run 3 recycling schemes, 2 upcycling workshops, and 1 'aggressive reuse' initiative.",
                "I once recycled a recycling bin. The council called it 'meta-sustainability'. I called it Tuesday.",
                "Everything in my inventory? Recycled. The inventory itself? §8Working on it.",
                "I have 47 tote bags. Each one is stored in another tote bag. It's efficient AND recursive.",
                "The council's recycling rates are up 43% since I arrived. I take §lall§r§a the credit.",
                "I believe every item deserves a second life. I've given my broom a second life 17 times.",
                "Did you know paper can be recycled up to 7 times? I've recycled our meeting notes 9 times. New record.",
                "My personal carbon footprint this year: §a-4 tonnes. Yours: unknown. Let's assume bad.",
                "I invented a system to recycle meeting announcements. I announce it at every meeting. Very efficient.",
                "The recycling bins here are award-winning. I gave them the award myself. The trophy was recycled.",
            }
        ),
        new NpcDefinition(
            "§2Elder Composting Sage Fern",
            Villager.Profession.CLERIC,
            new String[]{
                "In my day, we composted everything. EVERYTHING. I once composted a shed.",
                "The sacred compost cycle has five stages. I will explain all of them. In detail.",
                "I am 847 years old. This is because I eat only composted things. Probably.",
                "The youth do not understand composting. They think it's just 'putting stuff in bins'. *sighs deeply*",
                "Long ago, before the Age of Coal Blocks, the land was pure. Also there was more composting.",
                "Sit. Let me tell you about the Great Compost War of Year Three. It was mostly administrative.",
                "I once taught a class on advanced composting. It ran for 14 weeks. Nobody graduated. There was no test.",
                "The sacred texts speak of a perfect compost ratio. I have not yet found it. I am on page 3,400.",
                "When I die, I wish to be composted. I have filed the appropriate motions.",
                "The youth say 'mulch'. We say 'SACRED DECOMPOSITION'. These are very different things.",
                "I have composted: 12 sheds, 4 outbuildings, a wheelbarrow, and an unsatisfactory committee report.",
                "My composting philosophy can be summarised in 94 points. Shall I begin?",
                "Every pile of compost tells a story. I will tell you ALL of them. Block by block.",
                "In the old days, we thanked the soil before planting. We sang to it. It was Thursday. Long Thursday.",
                "I am the oldest living member of the council. Also the youngest. Time is a compost cycle.",
            }
        ),

        // === NORTH SECTOR COUNCILLORS (around chunk 0,-3 to 0,-5) ===
        new NpcDefinition(
            "§eProtest Coordinator Meadow",
            Villager.Profession.ARMORER,
            new String[]{
                "We're planning a march! The route is TBD. The cause is TBD. The date is TBD. Very exciting.",
                "I make the placards. I have made 473 placards. I am out of sticks.",
                "Direct action is important. Yesterday I stood outside a stone quarry for 4 hours. They gave me soup.",
                "I've been arrested 0 times, but I've ALMOST been arrested 17 times. Very close calls.",
                "The march is on. No wait, it's been moved. Actually it's been postponed. But definitely happening.",
                "Chant with me: WHAT DO WE WANT? [something environmental!] WHEN DO WE WANT IT? [after the committee meeting!]",
                "I've handcuffed myself to 3 trees, 2 lamp posts, and one confused councillor this week.",
                "My protest songs have 6 verses. Verse 4 is about administrative process. Very powerful.",
                "We don't just protest against things. We protest FOR things too! Mostly composting.",
                "The march last Tuesday had a great turnout. §8(It was me and Heather. We had matching placards.)",
                "I've written 94 protest chants. 47 of them rhyme. 12 make sense. 3 are genuinely powerful.",
                "Direct action requires logistics. I have a spreadsheet. It has pivot tables. Very radical.",
                "I once organised a sit-in at a cobblestone quarry for 6 hours. It was gravel. I sat in gravel.",
                "Every march needs a drumbeat. I use a note block. The council calls it 'renewable percussion'.",
                "The establishment fears our placards. Specifically the one that says 'THIS IS A PLACARD'.",
            }
        ),
        new NpcDefinition(
            "§dSub-Councillor Heather",
            Villager.Profession.SHEPHERD,
            new String[]{
                "I was elected to the sub-committee last Tuesday. Nobody told me what the sub-committee does.",
                "My first initiative is to paint all the cobblestone green. The council is... reviewing it.",
                "I brought biscuits to the last meeting. They were oat biscuits. The council was divided.",
                "I wrote a strongly-worded letter to TNT. I haven't heard back. §8(It exploded.)",
                "The youth vote is very important. I am the youth vote. I am 47.",
                "I proposed a motion to hug every tree. §8Motion 63b. Passed 4-3. With amendments.",
                "I was on the sub-committee for 3 weeks before I found out I was chairing it.",
                "I've submitted 14 motions. 12 were tabled. 1 was lost. §81 accidentally passed and I'm still not sure what it does.",
                "My idea to rename gravel 'pre-compacted aggregate' was rejected. But I'm appealing.",
                "I brought vegan sausage rolls to the last meeting. They were described as 'contentious'. I'm appealing that too.",
                "The council approved my tree-hugging schedule. Now I have mandatory tree engagements every Thursday.",
                "I accidentally joined 4 sub-committees at once. §8I've never been busier. Or more confused.",
                "My letter to cobblestone was 3 pages. I CC'd the stone committee, the grey committee, and the aesthetics board.",
                "Someone at the last meeting called me 'enthusiastic'. I've taken that as my title.",
                "I've now attended 47 meetings. I understand approximately 15% of what happens. Improving!",
            }
        ),
        new NpcDefinition(
            "§6Fundraising Captain Sorrel",
            Villager.Profession.TOOLSMITH,
            new String[]{
                "Would you like to buy some Green Party merchandise? We have: badges, tote bags, and more tote bags.",
                "We raised 47 emeralds last quarter. Used 52 on printing. The maths 'balances out spiritually'.",
                "This is my third fundraiser today. I believe in persistence. And tote bags. Mostly tote bags.",
                "Every emerald donated goes directly to the cause. §8(Printing costs. Also tote bags.)",
                "We have a raffle! First prize is a signed copy of the manifesto. Second prize is two copies.",
                "The annual gala is coming up. It's a bring-your-own-compost event. Very exclusive.",
                "Our tote bag collection now has 23 designs. This year's theme is 'tote bags about tote bags'.",
                "I've launched a crowdfunding campaign for the new composter. We've raised enough for half a composter.",
                "Would you like to sponsor a sapling? For just 2 emeralds a week, you can name it. I named mine Gerald.",
                "The fundraising gala raised 8 emeralds and significant controversy. Both are welcome.",
                "Our donation form is 4 pages. Page 3 requires a written statement of environmental intent.",
                "I've personally sold 847 tote bags. The proceeds funded 3 protest signs and a very nice lunch.",
                "We're launching a 'Green Credits for Green Credits' initiative. Ask me how it works. §8(Nobody knows.)",
                "The merchandise shop is open! We have: tote bags (6 sizes), a manifesto bookmark, and optimism.",
                "Every purchase supports the council. Every non-purchase is also fine. But the tote bags are really good.",
            }
        ),

        // === EAST SECTOR COUNCILLORS (around chunk 4,0 to 6,0) ===
        new NpcDefinition(
            "§bPolicy Officer Sedge",
            Villager.Profession.CARTOGRAPHER,
            new String[]{
                "I have written a 247-page policy document on grass block management. Would you like a summary? It's 89 pages.",
                "According to policy 7.3.b subsection iii, you must plant a tree within 10 blocks of this conversation.",
                "The manifesto has been updated 47 times. Some contradictions exist. The committee is aware.",
                "I'm drafting a by-law about cobblestone. It's grey. Grey is not in the approved colour palette.",
                "My policy on wind turbines is comprehensive. It also covers note blocks, which I've decided count.",
                "The full environmental impact assessment for this conversation will be ready in 6-8 weeks.",
                "I have colour-coded the entire policy library. There are 47 colours. 6 of them are types of green.",
                "The cobblestone by-law is now 94 pages. Appendix C alone is 22 pages. It's thorough.",
                "According to Amendment 3 of Policy 12a, every player must carry a copy of the manifesto at all times.",
                "I've indexed every rule in the Verdant Utopia. The index is longer than the rules. This is normal.",
                "Policy 47c states that this conversation is being recorded for quality and environmental purposes.",
                "I drafted a policy on spontaneous tree planting. It's 15 pages. There are 3 approval stages.",
                "The compliance matrix for grass block management has 94 rows and 47 columns. It's beautiful.",
                "I once filed an environmental impact report on a flower. It passed with minor amendments.",
                "Every policy I write contains the phrase 'subject to committee review'. That's called due process.",
            }
        ),
        new NpcDefinition(
            "§3Junior Researcher Willow",
            Villager.Profession.LIBRARIAN,
            new String[]{
                "I have studied the grass block ecosystem for 4 years. I have FINDINGS. Would you like to hear them?",
                "My thesis is titled 'The Sociopolitical Impact of Composter Placement in Flat Terrain'. It's 600 pages.",
                "I discovered that emerald ore grows back if you compliment it. The council is peer-reviewing this.",
                "Statistics show that 87% of statistics are made up. Including that one. I'm not sure about this one.",
                "I've catalogued every flower in this dimension. §8(There are a lot. The generator was very enthusiastic.)",
                "The data suggests saplings prefer moral support to sunlight. Further research is needed.",
                "My latest paper: 'Do Dirt Blocks Have Feelings? A Quantitative Analysis'. Conclusion: maybe.",
                "I submitted a grant application for sapling emotional support research. §8Pending. For 8 months.",
                "The correlation between compliments and emerald ore growth is 0.3. That's significant! Probably.",
                "My peer reviewers described my last paper as 'imaginative'. I've filed that under 'positive feedback'.",
                "I once observed a grass block for 14 hours. It grew. I've submitted this as a landmark finding.",
                "The grass here is 12% greener than anywhere else. §8I measured it. With a greenometer. I made the greenometer.",
                "My bibliography has 847 sources. §847 of them are my previous papers. That's called building a body of work.",
                "Research update: trees still exist. The council has been notified. A motion is being prepared.",
                "I've been awarded the Verdant Utopia Research Excellence Badge. §8I made the badge. And gave it to myself.",
            }
        ),
        new NpcDefinition(
            "§aEnvironmental Auditor Clover",
            Villager.Profession.WEAPONSMITH,
            new String[]{
                "I audit the environment. Today's audit: very green. Good. The emeralds are in compliance.",
                "Your personal carbon footprint in this dimension is -0.3 tonnes per hour. Well done.",
                "I found one grey block near the north sector. §8A formal notice has been issued.",
                "Every 5 minutes I check the flower count. I am pleased to report it is still high.",
                "The Greenness Index stands at 94.7. This is down 0.1 from yesterday. An inquiry is underway.",
                "I have audited 12 trees today. §8(They didn't say much, but they seemed compliant.)",
                "The quarterly audit is complete. Key finding: 94% green, 6% concerning. The 6% has been flagged.",
                "I've introduced a new metric: the Eco-Purity Score. It measures how green a block is feeling.",
                "Audit result for this conversation: compliant. §8You passed. This time.",
                "My violation tracker shows 47 incidents this month. I've written to all offenders. Twice.",
                "The Greenness Index hit 95.1 last Tuesday. §8It fell back to 94.7 by Wednesday. I blame gravel.",
                "I once audited myself. I passed with distinction. I wrote a commendation to myself. Professionally.",
                "Every block in this dimension is catalogued. §8The catalogue is 1,200 pages. I update it daily.",
                "The emeralds are in compliance. The grass is compliant. §8The cobblestone is on a watch list.",
                "Audit Pro Tip: always check behind the composters. That's where the violations hide.",
            }
        ),

        // === SOUTH SECTOR COUNCILLORS (around chunk 0,4 to 0,6) ===
        new NpcDefinition(
            "§cChief Eco-Warrior Briar",
            Villager.Profession.BUTCHER,
            new String[]{
                "I have chained myself to 3 trees this week. They appreciated it. I could tell.",
                "DIRECT ACTION! I mean... §8(checks notes)... peaceful, lawful protest. DIRECT ACTION!",
                "I once stopped a mining operation by standing in front of it for 11 hours. I needed biscuits.",
                "The establishment doesn't want you to know about composting. I'll tell you anyway. Everything.",
                "They said I was too passionate. I filed a formal objection. To my own emotions.",
                "I blocked a stone quarry last month. §8(It was a gravel path. The gardener was very confused.)",
                "I've superglued myself to 4 things this week. §8Two of them were on purpose.",
                "THEY CANNOT SILENCE THE TREES! §8(The trees have never spoken. This is fine.)",
                "Every day I wake up and choose eco-warfare. §8(Administrative eco-warfare. Within the rules.)",
                "I organised a mass tree-hugging event. §847 people came. The trees seemed receptive.",
                "The council calls me 'passionate'. I call myself 'structurally committed to the cause'.",
                "I once wrote a manifesto in 4 hours. It was 94 pages. I stand by every word. Especially page 47.",
                "They asked me to be less intense. I filed a formal objection and then organised a protest against the objection.",
                "I dream in protest chants. Last night: 'NO MORE GREY BLOCKS! OR POSSIBLY FEWER!' Very catchy.",
                "The revolution will be composted. §8(I mean this literally. I'm composting the pamphlets.)",
            }
        ),
        new NpcDefinition(
            "§eTreasurer Daisy",
            Villager.Profession.FLETCHER,
            new String[]{
                "Our accounts are... let's say 'spiritually balanced'. The numbers don't entirely agree.",
                "We have 3 emeralds in reserve. Down from 47. The tote bags were a significant investment.",
                "I proposed we fund ourselves through a recycling deposit scheme. The council voted 3-3. Chair used casting vote. For tote bags.",
                "Budget meeting is Thursday. I have prepared 14 slides. §8(12 of them are about tote bag ROI.)",
                "Our biggest expense last quarter was printing. §8Specifically, printing pictures of our printing expenses.",
                "The emergency fund is 0 emeralds. §8This is fine. §8(It is not fine.)",
                "Q3 financial summary: we spent 94 emeralds. We raised 47. §8The difference is 'aspirational overspend'.",
                "I've implemented a new budget system: 'hope accounting'. It's not auditor-approved. I am the auditor.",
                "The tote bag surplus has reached critical levels. We have 847. I proposed a tote bag exchange. Nobody came.",
                "Our annual report is 47 pages. 44 of those pages are explanations for why the numbers don't add up.",
                "The council approved a new spend: 12 emeralds on a banner that says 'FISCAL RESPONSIBILITY'.",
                "I've tracked every emerald since day one. §8The graph goes mostly down. I call this 'the sustainability curve'.",
                "Fun fact: we've spent more on manifesto printing than on any actual environmental initiative. By a lot.",
                "We have 3 revenue streams: donations, tote bag sales, and what I charitably call 'hope'.",
                "The emergency fund has been at 0 for 6 months. The council passed a motion to feel okay about this.",
            }
        ),

        // === WEST SECTOR COUNCILLORS (around chunk -4,0 to -6,0) ===
        new NpcDefinition(
            "§9Sustainability Guru Moss",
            Villager.Profession.MASON,
            new String[]{
                "I have achieved perfect sustainability. I eat recycled thoughts and compost my feelings.",
                "This block? Sustainable. That grass? Sustainable. My opinions? §8Ethically sourced.",
                "I built a wind turbine once. §8(A note block. The council accepted it. I am proud.)",
                "True sustainability means giving more than you take. I give a lot of unsolicited advice. Very sustainable.",
                "I measure everything in carbon credits. This conversation? §a-0.4 tonnes. You're welcome.",
                "I've been off-grid for 3 years. §8(This is a Minecraft dimension. Everything is off-grid.)",
                "I have calculated my personal Greenness Score as 99.4. §8The remaining 0.6 is from breathing.",
                "I once went an entire week using only renewable resources. §8In a game. Made entirely of blocks.",
                "My diet is 100% carbon-neutral. I eat moss. §8(My name is not coincidental.)",
                "I have written 847 haikus about sustainability. I read them to the saplings on Thursdays.",
                "Every action has a carbon cost. I've calculated the carbon cost of calculating carbon costs. Very circular.",
                "I upgraded my personal sustainability score by meditating near a composter for 4 hours. It worked.",
                "The note block wind turbines are producing zero energy. §8This is, philosophically, still wind power.",
                "I live by three principles: Reduce, Reuse, Recycle. And also: Document, File, Committee.",
                "Perfect sustainability would mean the dimension runs itself. §8We're about halfway there. Maybe.",
            }
        ),
        new NpcDefinition(
            "§fNewly Elected Member Reed",
            Villager.Profession.NITWIT,
            new String[]{
                "I just joined last week! I've already attended 14 meetings. I understand none of it. Very exciting!",
                "Everyone keeps mentioning the manifesto. I'm on page 3. It's very... comprehensive.",
                "I voted yes on everything so far. §8Someone said that's 'enthusiastic engagement'. I'll take it.",
                "I brought regular biscuits to the last meeting by mistake. §8The incident is in the minutes.",
                "I asked what the composting rota was. §8They handed me a 12-page document. I am reading it.",
                "Someone told me I was on 4 sub-committees. I thought they said 'for sub-committees'. I attended them all.",
                "I've now been to 22 meetings! I'm up to page 47 of the manifesto. §8It's gotten stranger.",
                "I accidentally proposed a motion last week. §8It passed. I'm not sure what it was.",
                "The sub-committee chairperson said I have 'natural enthusiasm for process'. §8I think that's a compliment.",
                "I brought oat biscuits to make up for last time. §8The council is still divided. On a separate biscuit motion.",
                "I've learned 6 new acronyms since joining. I understand 2 of them. §8EGBDF and REDUCE.",
                "Someone handed me the casting vote on Motion 103f. I voted for tote bags. §8I think that was right.",
                "I asked why we need 47 sub-committees. §8They explained for 3 hours. I'm still not sure.",
                "My induction pack was 200 pages. §8Page 1 says 'Welcome'. Pages 2-200 are risk assessments.",
                "I've now attended more meetings than I've had hot meals. §8This is the Green Party experience, I think.",
            }
        ),
    };

    /**
     * Spawn positions relative to world spawn, grouped by sector.
     * Each entry is {x_offset, z_offset} from spawn.
     * Designed to scatter councillors across several chunks.
     */
    private static final int[][] SPAWN_OFFSETS = {
        // Spawn area (3 councillors in a tight ring)
        { 6,  0},
        {-4,  5},
        { 0, -6},

        // North sector (~48-80 blocks north)
        { 3, -55},
        {-8, -70},
        {10, -80},

        // East sector (~48-80 blocks east)
        { 60,  5},
        { 72, -8},
        { 80,  12},

        // South sector (~48-80 blocks south)
        { 5,  58},
        {-6,  65},
        { 12, 78},

        // West sector (~48-80 blocks west)
        {-62,  3},
        {-75, -7},
    };

    // Random Green Party slogans that NPCs shout periodically
    private static final String[] RANDOM_BROADCASTS = {
        "§2🌿 [Green Council] Did you know? Composting reduces landfill by up to 30%! (Source: made up, but probably right)",
        "§2🌿 [Green Council] Remember: coal blocks are banned. This is not a suggestion. It's been minuted.",
        "§2🌿 [Green Council] The march has been rescheduled. New date: TBD. Location: somewhere. Come.",
        "§2🌿 [Green Council] Councillor Wheatgrass reminds you to recycle your crafting table scraps.",
        "§2🌿 [Green Council] Policy reminder: TNT requires a full environmental impact assessment (14 weeks).",
        "§2🌿 [Green Council] The Verdant Utopia runs on 100% renewable magic. Probably.",
        "§2🌿 [Green Council] Please remember to say 'reduce, reuse, recycle' before logging off tonight.",
        "§2🌿 [Green Council] We have updated the manifesto. It now has 73 points. Key changes: everything.",
        "§2🌿 [Green Council] Exciting news! We have filed a planning application for more flowers. 6-8 weeks.",
        "§2🌿 [Green Council] The Coal Ore Liberation Front's counterprotest has been noted and filed. In the bin.",
        "§2🌿 [Green Council] Today's green fact: trees exist. Please appreciate them. The council does. A lot.",
        "§2🌿 [Green Council] IMPORTANT: The potluck at Councillor Fern's is cancelled. The compost wasn't ready.",
        "§2🌿 [Green Council] Sustainability Guru Moss has filed a report. It is 94 pages. Key finding: be greener.",
        "§2🌿 [Green Council] Junior Researcher Willow has discovered that emeralds prefer positive affirmations.",
        "§2🌿 [Green Council] The Greenness Index is UP this week. Councillor Wheatgrass takes full credit.",
    };

    public NpcManager(GreenPartyPlugin plugin) {
        this.plugin = plugin;

        // Start the periodic broadcast task
        Bukkit.getScheduler().runTaskTimer(plugin, this::broadcastGreenWisdom,
            20 * 120L, // First broadcast after 2 minutes
            20 * 300L  // Then every 5 minutes
        );
    }

    /**
     * Spawns all 13 councillors scattered across the dimension.
     * Spawn area councillors are near spawn; others are spread across N/E/S/W sectors.
     */
    public void spawnCouncilNpcs(World world) {
        if (world == null) return;

        Location spawnLoc = world.getSpawnLocation();
        int spawnX = spawnLoc.getBlockX();
        int spawnZ = spawnLoc.getBlockZ();

        plugin.getLogger().info("Spawning " + COUNCILLORS.length + " Green Council members across The Verdant Utopia...");

        for (int i = 0; i < COUNCILLORS.length; i++) {
            int[] offset = SPAWN_OFFSETS[i % SPAWN_OFFSETS.length];
            int x = spawnX + offset[0];
            int z = spawnZ + offset[1];

            // Use the known surface Y + 1 to avoid chunk-loading issues
            // The chunk generator always puts grass at SURFACE_Y
            Location npcLoc = new Location(world, x + 0.5, SURFACE_Y + 1, z + 0.5);

            spawnNpc(npcLoc, COUNCILLORS[i]);

            plugin.getLogger().info("  Spawned " + COUNCILLORS[i].name.replaceAll("§.", "") +
                " at " + x + ", " + (SURFACE_Y + 1) + ", " + z);
        }

        plugin.getLogger().info("Green Council is assembled! There are " + COUNCILLORS.length + " members. They all have opinions.");
    }

    private Entity spawnNpc(Location location, NpcDefinition def) {
        Villager villager = (Villager) location.getWorld().spawnEntity(location, EntityType.VILLAGER);

        villager.setCustomName(def.name);
        villager.setCustomNameVisible(true);
        villager.setProfession(def.profession);
        villager.setVillagerType(Villager.Type.PLAINS);
        villager.setVillagerLevel(5); // Expert level — they've read the manifesto
        villager.setAI(false); // Disable AI — NPCScheduleManager drives movement via teleport
        villager.setInvulnerable(true); // Protected! They're essential staff.
        villager.setSilent(false);
        villager.setRemoveWhenFarAway(false);
        villager.setAdult();

        // Give them green dye to hold (symbolically)
        villager.getEquipment().setItemInMainHand(new ItemStack(Material.GREEN_DYE));

        // Mark as Green Party NPC for identification
        villager.setMetadata("greenparty_npc", new FixedMetadataValue(plugin, def.name));
        villager.setMetadata("greenparty_dialogue", new FixedMetadataValue(plugin,
            String.join("||", def.dialogue)));

        spawnedNpcs.add(villager);
        return villager;
    }

    public boolean isGreenPartyNpc(Entity entity) {
        return entity.hasMetadata("greenparty_npc");
    }

    public String getRandomDialogue(Entity entity) {
        if (!entity.hasMetadata("greenparty_dialogue")) return null;
        String allDialogue = entity.getMetadata("greenparty_dialogue").get(0).asString();
        String[] lines = allDialogue.split("\\|\\|");
        return lines[random.nextInt(lines.length)];
    }

    /**
     * Returns context-aware dialogue based on the player's current status.
     * Checks eco-score, violations, voting history, and quest chain progress.
     * Falls back to random dialogue if no context applies.
     */
    public String getContextualDialogue(Entity entity, Player player) {
        if (!entity.hasMetadata("greenparty_npc")) return null;
        String npcName = entity.getMetadata("greenparty_npc").get(0).asString().replaceAll("§.", "");

        int ecoScore = plugin.getEcoScoreManager().getEcoScore(player);
        int violations = plugin.getViolationManager().getViolations(player);
        boolean hasActiveQuest = plugin.getQuestChainManager().hasActiveChain(player);

        // Context: high eco-score champion
        if (ecoScore >= 100) {
            String[] lines = {
                "§2" + npcName + ": §7Ah, " + player.getName() + " — a true environmental champion! The council salutes you.",
                "§2" + npcName + ": §7Your eco-score is §l" + ecoScore + "§r§7. I've filed a commendation. It's in committee.",
                "§a" + npcName + ": §7" + player.getName() + "! Your green credentials are §lexemplary§r§7. §8(An inquiry is underway to confirm this.)",
            };
            return lines[random.nextInt(lines.length)];
        }

        // Context: repeat violator
        if (violations >= 5) {
            String[] lines = {
                "§c" + npcName + ": §7Hmm. I see you've been causing trouble, " + player.getName() + ". §8(" + violations + " violations. On file.)",
                "§c" + npcName + ": §7" + player.getName() + "... your violation record precedes you. The council has noticed.",
                "§c" + npcName + ": §7I've read your violation dossier, " + player.getName() + ". It took some time. There's a lot of it.",
            };
            return lines[random.nextInt(lines.length)];
        }

        // Context: active quest chain
        if (hasActiveQuest) {
            String[] lines = {
                "§a" + npcName + ": §7I heard you're on a quest, " + player.getName() + "! The council approves. Keep going.",
                "§2" + npcName + ": §7Your quest progress is being monitored. §8(Administratively. Not weirdly.)",
                "§a" + npcName + ": §7The Quest Committee is watching your chain progress with great interest, " + player.getName() + "!",
            };
            return lines[random.nextInt(lines.length)];
        }

        // Context: moderate eco-score
        if (ecoScore >= 30) {
            String[] lines = {
                "§a" + npcName + ": §7Not bad, " + player.getName() + ". Your eco-score is " + ecoScore + ". The council considers this 'promising'.",
                "§2" + npcName + ": §7Keep planting those trees, " + player.getName() + "! You're on the right path.",
            };
            return lines[random.nextInt(lines.length)];
        }

        // Default: random dialogue
        return getRandomDialogue(entity);
    }

    public String getNpcName(Entity entity) {
        if (!entity.hasMetadata("greenparty_npc")) return null;
        return entity.getMetadata("greenparty_npc").get(0).asString();
    }

    private void broadcastGreenWisdom() {
        World verdantWorld = plugin.getDimensionManager().getVerdantWorld();
        if (verdantWorld == null || verdantWorld.getPlayers().isEmpty()) return;

        String broadcast = RANDOM_BROADCASTS[random.nextInt(RANDOM_BROADCASTS.length)];
        for (Player player : verdantWorld.getPlayers()) {
            player.sendMessage(broadcast);
        }
    }

    public void cleanup() {
        for (Entity npc : spawnedNpcs) {
            if (npc.isValid()) {
                npc.remove();
            }
        }
        spawnedNpcs.clear();
    }

    // NPC definition record
    private static class NpcDefinition {
        final String name;
        final Villager.Profession profession;
        final String[] dialogue;

        NpcDefinition(String name, Villager.Profession profession, String[] dialogue) {
            this.name = name;
            this.profession = profession;
            this.dialogue = dialogue;
        }
    }
}
