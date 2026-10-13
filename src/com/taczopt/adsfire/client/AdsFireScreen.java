package com.taczopt.adsfire.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.taczopt.TaczOptimizeMod;
import com.taczopt.adsfire.AdsFireConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdsFireScreen extends Screen {

    private static final int ROW_H = 26;

    private static final int HEADER_H = 58;
    private static final int FOOTER_H = 28;

    private static final int HEAD_X = 50;

    private static final int TITLE_DX = 30;

    private static final int TITLE_Y = 17;

    private static final int BAR_X = 10;

    private static final int BAR_W = 2;

    private static final int NAME_X = 80;

    private static final int TOGGLE_W = 46;

    private static final int TOGGLE_RIGHT_GAP = 10;

    private final List<AdsFireConfig.GunEntry> all = new ArrayList<AdsFireConfig.GunEntry>();
    private final List<AdsFireConfig.GunEntry> shown = new ArrayList<AdsFireConfig.GunEntry>();

    private EditBox search;
    private Button globalButton;
    private Button clearButton;

    private int panelX;
    private int panelW;
    private int panelH;
    private int listX;
    private int listY;
    private int listW;
    private int listH;
    private int perPage;
    private int scroll = 0;

    private int nameX = 0;

    private int toggleX = 0;

    private int toggleW = TOGGLE_W;

    private int leftX = HEAD_X;

    public AdsFireScreen() {
        super(Component.m_237115_("taczadsfire.screen.title"));
    }

    @Override
    protected void m_7856_() {
        this.all.clear();
        this.all.addAll(AdsFireConfig.listGuns());

        final int MARGIN = 6;
        int availW = this.f_96543_;
        int w = Math.max(120, availW - MARGIN * 2);
        this.panelX = Math.max(0, Math.min(MARGIN, availW - w));
        this.panelW = w;
        int availH = this.f_96544_;
        int h = Math.max(100, availH - MARGIN * 2);
        int top = Math.max(0, Math.min(MARGIN, availH - h));
        this.panelH = h;

        this.listX = this.panelX;
        this.listY = top + HEADER_H;
        this.listW = w;
        this.listH = h - HEADER_H - FOOTER_H;
        this.perPage = Math.max(1, this.listH / ROW_H);

        int leftX0 = Math.min(HEAD_X, Math.max(0, w / 3));
        this.leftX = leftX0;

        int minName = 70;
        this.toggleW = TOGGLE_W;
        int txRel = w - TOGGLE_RIGHT_GAP - this.toggleW;
        int nameRel = Math.min(NAME_X, Math.max(0, txRel - 8 - minName));
        if (txRel - 8 - nameRel < minName) {

            this.toggleW = Math.max(30, this.toggleW - (minName - (txRel - 8 - nameRel)));
            txRel = w - TOGGLE_RIGHT_GAP - this.toggleW;
            nameRel = Math.min(nameRel, Math.max(0, txRel - 8 - minName));
        }
        this.toggleX = txRel;
        this.nameX = nameRel;

        TaczOptimizeMod.logger().info(
                "[taczopt] 布局: screen={}x{} panelX={} panelW={} listX={} listY={} "
                        + "barX(abs)={} nameX(abs)={} toggleX(abs)={} toggleW={} perPage={}",
                this.f_96543_, this.f_96544_, this.panelX, w, this.listX, this.listY,
                this.listX + BAR_X, this.listX + this.nameX, this.listX + this.toggleX,
                this.toggleW, this.perPage);

        this.search = new EditBox(this.f_96547_, this.panelX + 10, top + 30, Math.max(60, w - 20), 18,
                Component.m_237115_("taczadsfire.screen.search"));
        this.search.m_94144_("");
        this.search.m_93692_(false);
        this.m_142416_(this.search);

        int btnW = Math.max(50, (w - 30) / 2);
        int btnY = top + h - 24;

        this.globalButton = makeButton(this.panelX + 10, btnY, btnW, 20,
                Component.m_237115_(""), b -> {
                    AdsFireConfig.setDefaultOn(!AdsFireConfig.defaultOn());
                    refreshTexts();
                });
        this.m_142416_(this.globalButton);

        this.clearButton = makeButton(this.panelX + w - 10 - btnW, btnY, btnW, 20,
                Component.m_237115_("taczadsfire.screen.clear"), b -> {
                    AdsFireConfig.clearAll();
                    applyFilter();
                    refreshTexts();
                });
        this.m_142416_(this.clearButton);

        applyFilter();
        refreshTexts();
    }

    private static Button makeButton(int x, int y, int w, int h, Component label, Button.OnPress onPress) {
        return Button.m_253074_(label, onPress)
                .m_252987_(x, y, w, h)
                .m_252778_(sup -> (net.minecraft.network.chat.MutableComponent) sup.get())
                .m_253136_();
    }

    private void refreshTexts() {
        if (this.globalButton != null) {
            this.globalButton.m_93666_(Component.m_237115_(AdsFireConfig.defaultOn()
                    ? "taczadsfire.screen.default_on" : "taczadsfire.screen.default_off"));
        }
    }

    private void applyFilter() {
        String q = this.search == null ? "" : this.search.m_94155_().trim().toLowerCase(Locale.ROOT);
        this.shown.clear();
        for (AdsFireConfig.GunEntry e : this.all) {
            if (q.isEmpty()
                    || e.displayName().toLowerCase(Locale.ROOT).contains(q)
                    || e.id.toLowerCase(Locale.ROOT).contains(q)) {
                this.shown.add(e);
            }
        }
        this.scroll = 0;
        layoutRows();
    }

    private void layoutRows() {
        int maxScroll = Math.max(0, this.shown.size() - this.perPage);
        if (this.scroll > maxScroll) {
            this.scroll = maxScroll;
        }
        if (this.scroll < 0) {
            this.scroll = 0;
        }
    }

    private int rowY(int slot) {
        return this.listY + slot * ROW_H + 2;
    }

    private int toggleX() {
        return this.toggleX;
    }

    @Override
    public void m_88315_(GuiGraphics g, int mouseX, int mouseY, float partial) {
        this.m_280039_(g);
        super.m_88315_(g, mouseX, mouseY, partial);

        int titleX = Math.min(this.leftX + TITLE_DX, Math.max(0, this.listW - 40));
        g.m_280137_(this.f_96547_, this.f_96539_.getString(),
                titleX, this.listY - HEADER_H + TITLE_Y, 0xFFFFFFFF);

        g.m_280509_(this.listX + 8, this.listY - 2, this.listX + this.listW - 8, this.listY + this.listH + 2, 0x66000000);

        if (this.all.isEmpty()) {
            g.m_280137_(this.f_96547_, Component.m_237115_("taczadsfire.screen.empty").getString(),
                    this.listX + 12, this.listY + 6, 0xFFFFAAAA);
            return;
        }
        if (this.shown.isEmpty()) {
            g.m_280137_(this.f_96547_, Component.m_237115_("taczadsfire.screen.nomatch").getString(),
                    this.listX + 12, this.listY + 6, 0xFFFFAAAA);
            return;
        }

        int tx = this.listX + toggleX();
        int nameAbs = this.listX + this.nameX;
        int rowH = ROW_H - 4;
        for (int i = 0; i < this.perPage; i++) {
            int idx = this.scroll + i;
            if (idx >= this.shown.size()) {
                break;
            }
            AdsFireConfig.GunEntry e = this.shown.get(idx);
            int y = rowY(i);
            boolean on = AdsFireConfig.isOn(e.id);

            boolean hoverRow = mouseX >= this.listX + BAR_X && mouseX < tx - 4 && mouseY >= y && mouseY < y + rowH;
            if (hoverRow) {
                g.m_280509_(this.listX + BAR_X, y, tx - 4, y + rowH, 0x33FFFFFF);
            }
            g.m_280509_(this.listX + BAR_X, y, this.listX + BAR_X + BAR_W, y + rowH, on ? 0xFF6FE06F : 0xFF666666);

            String name = trim(e.displayName(), tx - 8 - nameAbs);
            g.m_280137_(this.f_96547_, name, nameAbs, y + 7,
                    on ? 0xFFFFE08A : 0xFF9A9A9A);

            boolean hoverT = mouseX >= tx && mouseX < tx + this.toggleW && mouseY >= y && mouseY < y + rowH;
            int bg = on ? (hoverT ? 0xFF3E8B3E : 0xFF2E6B2E) : (hoverT ? 0xFF7A3E3E : 0xFF5A2E2E);
            int border = on ? 0xFF6FE06F : 0xFFE06F6F;
            g.m_280509_(tx, y, tx + this.toggleW, y + rowH, bg);
            g.m_280509_(tx, y, tx + this.toggleW, y + 1, border);
            g.m_280509_(tx, y + rowH - 1, tx + this.toggleW, y + rowH, border);
            g.m_280509_(tx, y, tx + 1, y + rowH, border);
            g.m_280509_(tx + this.toggleW - 1, y, tx + this.toggleW, y + rowH, border);
            String label = Component.m_237115_(on ? "taczadsfire.screen.on" : "taczadsfire.screen.off").getString();
            int lw = this.f_96547_.m_92895_(label);
            g.m_280137_(this.f_96547_, label, tx + (this.toggleW - lw) / 2, y + (rowH - 8) / 2, 0xFFFFFFFF);
        }

        int total = this.shown.size();
        if (total > this.perPage) {
            int barH = Math.max(12, this.listH * this.perPage / total);
            int barY = this.listY + (this.listH - barH) * this.scroll / Math.max(1, total - this.perPage);
            g.m_280509_(this.listX + this.listW - 4, this.listY, this.listX + this.listW - 1, this.listY + this.listH, 0x44FFFFFF);
            g.m_280509_(this.listX + this.listW - 4, barY, this.listX + this.listW - 1, barY + barH, 0xCCFFFFFF);
        }
    }

    private String trim(String s, int width) {
        if (width <= 8 || this.f_96547_.m_92895_(s) <= width) {
            return s;
        }
        return this.f_96547_.m_92834_(s, Math.max(4, width - 6)) + "...";
    }

    @Override
    public boolean m_6375_(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int tx = this.listX + toggleX();
            int rowH = ROW_H - 4;
            for (int i = 0; i < this.perPage; i++) {
                int idx = this.scroll + i;
                if (idx >= this.shown.size()) {
                    break;
                }
                int y = rowY(i);
                if (mouseY < y || mouseY >= y + rowH) {
                    continue;
                }

                if ((mouseX >= this.listX + BAR_X && mouseX < tx - 4)
                        || (mouseX >= tx && mouseX < tx + this.toggleW)) {
                    AdsFireConfig.toggle(this.shown.get(idx).id);
                    return true;
                }
            }
        }
        return super.m_6375_(mouseX, mouseY, button);
    }

    @Override
    public boolean m_6050_(double mouseX, double mouseY, double delta) {
        int maxScroll = Math.max(0, this.shown.size() - this.perPage);
        if (maxScroll > 0 && mouseX >= this.listX && mouseX <= this.listX + this.listW
                && mouseY >= this.listY && mouseY <= this.listY + this.listH) {
            int step = delta > 0 ? -1 : 1;
            this.scroll = Mth.m_14045_(this.scroll + step, 0, maxScroll);
            layoutRows();
            return true;
        }
        return super.m_6050_(mouseX, mouseY, delta);
    }

    @Override
    public boolean m_5534_(char code, int key) {
        if (this.search != null && this.search.m_93696_()) {
            boolean handled = this.search.m_5534_(code, key);
            applyFilter();
            return handled;
        }
        return super.m_5534_(code, key);
    }

    @Override
    public boolean m_7933_(int key, int scan, int mods) {
        if (key == 256) {
            this.m_7379_();
            return true;
        }
        return super.m_7933_(key, scan, mods);
    }

    @Override
    public void m_7379_() {

        super.m_7379_();
    }
}
