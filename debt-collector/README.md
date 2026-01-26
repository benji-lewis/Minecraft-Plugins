# Debt Collector Plugin

Spawns "Debt Collector" NPCs in suits that chase and attack players whose Vault economy balance drops below **£0.00**.

## Features
- Checks player balances via Vault.
- Spawns a single debt collector per indebted player.
- Collectors wear black leather armor ("suit") and are named `Debt Collector <random name>`.
- Collectors chase and attack the indebted player until their balance is non-negative.

## Requirements
- Paper 1.19+ (tested against API 1.19.4)
- Vault
- An economy plugin that Vault can hook into

## Installation
1. Build the plugin with Maven.
2. Drop the resulting JAR into your server's `plugins` folder.
3. Ensure Vault and an economy plugin are installed.

## Build
```bash
mvn clean package
```

## Notes
- Debt collectors are tagged internally and removed when a player is no longer in debt or leaves the server.
