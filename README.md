# Demistorm Template
Template project for MC 1.21.11 using Architectury Loom for multiloader support (Fabric, NeoForge, and Forge setup already).

Vivecraft optional dependency commented out in respective loader build.gradle scripts and VivecraftGate present to easily setup Vivecraft-optional mods.
Includes some example mixins so they are already wired in. 

By default, ```gradlew build``` will copy production jars to /build/dist/ in the main directory for ease of access.

There is a publish workflow in .github/workflows/publish.yml that massively simplifies version releases and updates once a project has already been published before per-platform. Just need to replace "Template" with accurate page values and remember to setup the proper API key secrets on GitHub :)

*Uploading here for simplicity and ease of life across machines :P*