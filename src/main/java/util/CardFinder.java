package util;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CardFinder {
    private static final Point FIRST_CARD_COORDINATE = new Point(143, 586);
    private static final int CARD_WIDTH = 62;
    private static final int CARD_HEIGHT = 87;
    private static final int CARD_STEP = 72;
    private static final Point CARD_CHECK_COORDINATE = new Point(40, 21);
    private static final int WHITE_BACKGROUND_RGB = -1;
    private static final int DARKENED_BACKGROUND_RGB = -8882056;
    private static final Point CARD_SUIT_COORDINATE = new Point(23, 45);
    private static final int CARD_SUIT_WIDTH = 37;
    private static final int CARD_SUIT_HEIGHT = 39;
    private static final int CARD_VALUE_WIDTH = 35;
    private static final int CARD_VALUE_HEIGHT = 31;
    private final Map<String, BufferedImage> valueTemplateMap;
    private final Map<String, BufferedImage> suitTemplateMap;

    public CardFinder(Map<String, BufferedImage> valueTemplateMap, Map<String, BufferedImage> suitTemplateMap) {
        this.valueTemplateMap = valueTemplateMap;
        this.suitTemplateMap = suitTemplateMap;
    }

    public String getCardSequence(BufferedImage image) {
        List<BufferedImage> cards = new ArrayList<>();
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            int rgb = image.getRGB(FIRST_CARD_COORDINATE.x + i * CARD_STEP + CARD_CHECK_COORDINATE.x, FIRST_CARD_COORDINATE.y + CARD_CHECK_COORDINATE.y);
            if (rgb != WHITE_BACKGROUND_RGB && rgb != DARKENED_BACKGROUND_RGB) break;
            cards.add(image.getSubimage(FIRST_CARD_COORDINATE.x + i * CARD_STEP, FIRST_CARD_COORDINATE.y, CARD_WIDTH, CARD_HEIGHT));
        }
        for (BufferedImage card : cards) {
            result.append(determineCard(card));
        }
        return result.toString();
    }

    private String determineCard(BufferedImage image) {
        BufferedImage value = image.getSubimage(0, 0, CARD_VALUE_WIDTH, CARD_VALUE_HEIGHT);
        BufferedImage suit = image.getSubimage(CARD_SUIT_COORDINATE.x, CARD_SUIT_COORDINATE.y, CARD_SUIT_WIDTH, CARD_SUIT_HEIGHT);
        Map<Float, String> fittingImageMap = new HashMap<>();
        for (Map.Entry<String, BufferedImage> entry : valueTemplateMap.entrySet()) {
            float fittingFactor = getFittingFactor(value, entry.getValue(), 0.1f);
            if (fittingFactor > 0) fittingImageMap.put(fittingFactor, entry.getKey());
        }
        Optional<String> optional = fittingImageMap.entrySet().stream().max(Map.Entry.comparingByKey()).map(Map.Entry::getValue);
        if (!optional.isPresent()) throw new RuntimeException("Could not find value of the card: " + image);
        String cardSequence = optional.get();
        fittingImageMap.clear();
        for (Map.Entry<String, BufferedImage> entry : suitTemplateMap.entrySet()) {
            float fittingFactor = getFittingFactor(suit, entry.getValue(), 0.08f);
            if (fittingFactor > 0) fittingImageMap.put(fittingFactor, entry.getKey());
        }
        optional = fittingImageMap.entrySet().stream().max(Map.Entry.comparingByKey()).map(Map.Entry::getValue);
        if (!optional.isPresent()) throw new RuntimeException("Could not find suit of the card: " + image);
        cardSequence += optional.get();
        return cardSequence;
    }

    private float getFittingFactor(BufferedImage image, BufferedImage targetImage, float incompatibilityFactor) {
        if (targetImage.getWidth() > image.getWidth() || targetImage.getHeight() > image.getHeight()) return -1;
        float resultMistakeCount = Integer.MAX_VALUE;
        int maxMistakeCount = (int) (targetImage.getHeight() * targetImage.getWidth() * incompatibilityFactor);
        int mistakeCount;
        for (int i = 0; i <= image.getWidth() - targetImage.getWidth() ; i++) {
            for (int j = 0; j <= image.getHeight() - targetImage.getHeight() ; j++) {
                mistakeCount = 0;
                out:
                for (int k = 0; k < targetImage.getWidth(); k++) {
                    for (int l = 0; l < targetImage.getHeight(); l++) {
                        boolean isBack = image.getRGB(k + i, l + j) == WHITE_BACKGROUND_RGB || image.getRGB(k + i, l + j) == DARKENED_BACKGROUND_RGB;
                        if (targetImage.getRGB(k, l) == WHITE_BACKGROUND_RGB && isBack) continue;
                        if (targetImage.getRGB(k, l) != WHITE_BACKGROUND_RGB && !isBack) continue;
                        mistakeCount++;
                        if (mistakeCount > maxMistakeCount) break out;
                    }
                }
                if (mistakeCount <= maxMistakeCount && mistakeCount < resultMistakeCount) resultMistakeCount = mistakeCount;
            }
        }
        if (resultMistakeCount == Integer.MAX_VALUE) return -1;
        return (targetImage.getHeight() * targetImage.getWidth() - resultMistakeCount) / (targetImage.getHeight() * targetImage.getWidth());
    }
}