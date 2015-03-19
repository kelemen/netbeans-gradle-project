package org.netbeans.gradle.project.properties;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionAdapter;
import java.util.concurrent.TimeUnit;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import org.jtrim.cancel.CancellationController;
import org.jtrim.property.swing.DelayedGlassPane;
import org.jtrim.property.swing.GlassPaneFactory;
import org.jtrim.utils.ExceptionHelper;

import static org.jtrim.property.swing.AutoDisplayState.invisibleGlassPane;

public final class GlassPanes {
    private static final Color LOADING_PANEL_BASE_BCKG = Color.GRAY;
    private static final Color LOADING_PANEL_BCKG = toTransparent(LOADING_PANEL_BASE_BCKG);

    private static void registerInputBlockers(Component component) {
        component.addMouseListener(new MouseAdapter() { });
        component.addMouseMotionListener(new MouseMotionAdapter() { });
        component.addMouseWheelListener(new MouseAdapter() { });
        component.addKeyListener(new KeyAdapter() { });
    }

    public static GlassPaneFactory loadingPanel(
            final String loadingCaption,
            final CancellationController cancelTask) {

        ExceptionHelper.checkNotNullArgument(loadingCaption, "loadingCaption");

        return new GlassPaneFactory() {
            @Override
            public JPanel createGlassPane() {
                JPanel result = new JPanel(new GridBagLayout());
                registerInputBlockers(result);

                result.setBackground(LOADING_PANEL_BCKG);
                result.setOpaque(false);

                TextWithCancelPanel panel = new TextWithCancelPanel(loadingCaption, cancelTask);
                panel.setOpaque(true);
                panel.setBackground(LOADING_PANEL_BASE_BCKG);

                JLabel loadingLabel = panel.getPanelTextComponent();

                Font font = loadingLabel.getFont().deriveFont(24.0f);
                loadingLabel.setBackground(LOADING_PANEL_BCKG);
                loadingLabel.setFont(font);
                loadingLabel.setHorizontalAlignment(SwingConstants.CENTER);
                loadingLabel.setHorizontalTextPosition(SwingConstants.CENTER);
                loadingLabel.setVerticalTextPosition(SwingConstants.CENTER);

                result.add(panel, new GridBagConstraints());

                return result;
            }
        };
    }

    public static DelayedGlassPane delayGlassPane(GlassPaneFactory mainGlassPane) {
        return new DelayedGlassPane(invisibleGlassPane(), mainGlassPane, 200, TimeUnit.MILLISECONDS);
    }

    public static GlassPaneFactory loadingPanel(String loadingCaption) {
        return loadingPanel(loadingCaption, null);
    }

    public static DelayedGlassPane delayedLoadingPanel(String loadingCaption, CancellationController cancelTask) {
        ExceptionHelper.checkNotNullArgument(cancelTask, "cancelTask");

        return delayGlassPane(loadingPanel(loadingCaption, cancelTask));
    }

    public static DelayedGlassPane delayedLoadingPanel(String loadingCaption) {
        return delayGlassPane(loadingPanel(loadingCaption, null));
    }

    private static Color toTransparent(Color color) {
        int transparency = 0x80;
        return new Color((color.getRGB() & 0x00FF_FFFF) | (transparency << 24), true);
    }

    private GlassPanes() {
        throw new AssertionError();
    }
}

