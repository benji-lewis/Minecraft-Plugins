package uk.greenparty.world;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import java.util.Random;

/**
 * Generates "The Verdant Utopia" — a flat, overwhelmingly green world.
 *
 * Layer composition (from bottom to top):
 * Y 0-5:     Bedrock (even the Green Party can't repeal bedrock)
 * Y 6-50:    Stone (we tried replacing it with recycled materials, it didn't load)
 * Y 51-54:   Dirt (sacred, do not touch without a permit)
 * Y 55:      Grass Block (THE foundation of Green Party policy)
 * Y 56+:     Air (clean, green, with a slight smell of compost)
 *
 * Special features:
 * - Scattered emerald deposits because obviously
 * - Occasional flower patches (up to 47% of chunks, by council decree)
 * - Small forests of oak trees that will definitely not be cut down
 * - Absolutely zero coal (banned, see §4 of the Green Party Dimension Manifesto)
 */
public class VerdantChunkGenerator extends ChunkGenerator {

    private static final int BEDROCK_LAYERS = 5;
    private static final int STONE_TOP = 50;
    private static final int DIRT_TOP = 54;
    private static final int SURFACE = 55;

    @Override
    public void generateSurface(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkData chunkData) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                // Bedrock base (non-negotiable, even by the Green Party)
                for (int y = 0; y < BEDROCK_LAYERS; y++) {
                    chunkData.setBlock(x, y, z, Material.BEDROCK);
                }

                // Stone layer (the backbone of the utopia)
                for (int y = BEDROCK_LAYERS; y <= STONE_TOP; y++) {
                    chunkData.setBlock(x, y, z, Material.STONE);
                }

                // Dirt layer (sacred compostable material)
                for (int y = STONE_TOP + 1; y < SURFACE; y++) {
                    chunkData.setBlock(x, y, z, Material.DIRT);
                }

                // Grass surface (THE GREEN SURFACE)
                chunkData.setBlock(x, SURFACE, z, Material.GRASS_BLOCK);

                // Scatter emeralds because we're wealthy in environmental capital
                if (random.nextInt(100) < 5) {
                    int emeraldY = BEDROCK_LAYERS + random.nextInt(STONE_TOP - BEDROCK_LAYERS);
                    chunkData.setBlock(x, emeraldY, z, Material.EMERALD_ORE);
                }
            }
        }

        // Decorate the surface with green things
        decorateSurface(random, chunkX, chunkZ, chunkData);
    }

    private void decorateSurface(Random random, int chunkX, int chunkZ, ChunkData chunkData) {
        // Place flowers and grass
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int roll = random.nextInt(100);

                if (roll < 15) {
                    // Tall grass — very on brand
                    chunkData.setBlock(x, SURFACE + 1, z, Material.SHORT_GRASS);
                } else if (roll < 20) {
                    // Dandelions — the flower of protest
                    chunkData.setBlock(x, SURFACE + 1, z, Material.DANDELION);
                } else if (roll < 25) {
                    // Poppies — for the fallen trees
                    chunkData.setBlock(x, SURFACE + 1, z, Material.POPPY);
                } else if (roll < 28) {
                    // Blue orchids — endangered, but thriving here
                    chunkData.setBlock(x, SURFACE + 1, z, Material.BLUE_ORCHID);
                } else if (roll < 31) {
                    // Ferns — very environmental
                    chunkData.setBlock(x, SURFACE + 1, z, Material.FERN);
                } else if (roll < 33) {
                    // Oxeye daisy — the councillors love them
                    chunkData.setBlock(x, SURFACE + 1, z, Material.OXEYE_DAISY);
                }
            }
        }

        // Occasionally place a tree (this chunk gets to vote on it)
        if (random.nextInt(100) < 30) {
            int treeX = 2 + random.nextInt(12);
            int treeZ = 2 + random.nextInt(12);
            placeTree(treeX, treeZ, chunkData, random);
        }

        // Place a composter in some chunks (the holy relic)
        if (random.nextInt(100) < 10) {
            int cx = random.nextInt(16);
            int cz = random.nextInt(16);
            chunkData.setBlock(cx, SURFACE + 1, cz, Material.COMPOSTER);
        }

        // Occasionally place green wool patches (campaign banners)
        if (random.nextInt(100) < 8) {
            int bx = random.nextInt(16);
            int bz = random.nextInt(16);
            chunkData.setBlock(bx, SURFACE + 1, bz, Material.GREEN_WOOL);
        }
    }

    private void placeTree(int x, int z, ChunkData chunkData, Random random) {
        int height = 4 + random.nextInt(3);

        // Trunk
        for (int y = 1; y <= height; y++) {
            chunkData.setBlock(x, SURFACE + y, z, Material.OAK_LOG);
        }

        // Leaves (big and bushy, for maximum carbon sequestration)
        int leafTop = SURFACE + height;
        for (int lx = -2; lx <= 2; lx++) {
            for (int lz = -2; lz <= 2; lz++) {
                for (int ly = leafTop - 2; ly <= leafTop + 1; ly++) {
                    int nx = x + lx;
                    int nz = z + lz;
                    if (nx >= 0 && nx < 16 && nz >= 0 && nz < 16) {
                        if (!(lx == 0 && lz == 0) || ly > leafTop) {
                            // Just place leaves — safe to overwrite air/grass in chunk gen
                            if (true) {
                                chunkData.setBlock(nx, ly, nz, Material.OAK_LEAVES);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean shouldGenerateStructures() {
        // No structures — everything requires planning permission
        // The committee is still meeting
        return false;
    }

    @Override
    public boolean shouldGenerateCaves() {
        // No caves — too dark and underground activities have been
        // deemed "environmentally concerning" by the council
        return false;
    }

    @Override
    public boolean shouldGenerateMobs() {
        return false;
    }

    @Override
    public boolean shouldGenerateDecorations() {
        return false; // We handle our own decorations, thank you
    }
}
