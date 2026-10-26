package ru.safimine.asteria.visuals.mc;

import ru.safimine.asteria.visuals.Game;

/** public access to the package-private game adapter (for mixins) */
public final class Bridge { private Bridge() {} public static Game game() { return Game121.I; } }
