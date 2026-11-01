package ru.safimine.asteria.visuals;

// Checks of the chat coordinates detection (Fx.findCoords: the "⌖ метка" link after chat lines). Not part of the mod; run with:
//   javac -encoding UTF-8 -proc:none -cp <gson.jar> -d out $(find mods/common-visuals -name '*.java') mods/toolchain/ChatCoordsTest.java
//   java -cp "out;<gson.jar>" ru.safimine.asteria.visuals.ChatCoordsTest
public final class ChatCoordsTest {
    private static int failed;

    private static void check(String line, String want) {
        String got = Fx.findCoords(line);
        boolean ok = want == null ? got == null : want.equals(got);
        if (!ok) failed++;
        System.out.println((ok ? "ok    " : "FAIL  ") + line + "  ->  " + got + (ok ? "" : "  (want " + want + ")"));
    }

    public static void main(String[] args) {
        // real coordinates
        check("тп на 100 80 -2010", "100 80 -2010");
        check("x: 12, y: 64, z: -5", "12 64 -5");
        check("X=12 Y=64 Z=-5", "12 64 -5");
        check("База: 1500 / 70 / -320", "1500 70 -320");
        check("[100, 64, -200]", "100 64 -200");
        check("(100 64 -200)", "100 64 -200");
        check("100,64,-200", "100 64 -200");
        check("x 100 z -200", "100 ~ -200");
        check("x: 100, z: -200", "100 ~ -200");
        check("встречаемся (1500, -320)", "1500 ~ -320");
        check("go 0 64 0", "0 64 0");
        // money and other numbers
        check("SAFIMINE ≫ Вам выдано 5 000,00 ⛀", null);
        check("SAFIMINE ≫ Вам выдано 5 000,00 ⛀", null);
        check("SAFIMINE ≫ Вам выдано 5 000,00 ⛀", null);
        check("Баланс: 1 250 300 монет", null);
        check("Вы получили 100 64 200 сапфиров", null);
        check("Цена 10 20 30 ₽", null);
        check("Выигрыш 100 200 300 ꒙", null);
        check("Выигрыш 100 200 300ꒉ", null);
        check("Вам выдано 12 345 678,50", null);
        check("Координаты 100.5 64 -200.3", null);
        check("Страница [1/3]", null);
        check("Баланс (1 500)", null);
        check("Онлайн 12 из 100", null);
        check("версия 1.20.1", null);
        check("в 2026 году 15 игроков", null);
        check("числа 100 999 300", null);
        check("Шанс 10 20 30%", null);
        check("дом5 64 100", null);
        System.out.println(failed == 0 ? "all passed" : failed + " failed");
        if (failed != 0) System.exit(1);
    }
}
