package io.github.darealturtywurty.superturtybot.database.pojos;

public class RankCard {
    private MongoColor backgroundColor;
    private MongoColor outlineColor;
    private MongoColor rankTextColor;
    private MongoColor levelTextColor;
    private MongoColor xpOutlineColor;
    private MongoColor xpEmptyColor;
    private MongoColor xpFillColor;
    private MongoColor avatarOutlineColor;
    private MongoColor percentTextColor;
    private MongoColor xpTextColor;
    private MongoColor nameTextColor;

    private String backgroundImage;
    private String outlineImage;
    private String xpEmptyImage;
    private String xpOutlineImage;
    private String xpFillImage;
    private String avatarOutlineImage;

    private float outlineOpacity;

    public RankCard() {
        this.backgroundColor = new MongoColor(46, 67, 71);
        this.outlineColor = new MongoColor(28, 33, 48);
        this.rankTextColor = new MongoColor(255, 61, 127);
        this.levelTextColor = new MongoColor(245, 223, 152);
        this.xpOutlineColor = new MongoColor(163, 184, 8);
        this.xpEmptyColor = new MongoColor(81, 149, 72);
        this.xpFillColor = new MongoColor(136, 196, 37);
        this.avatarOutlineColor = new MongoColor(0, 0, 0);
        this.percentTextColor = new MongoColor(190, 242, 2);
        this.xpTextColor = new MongoColor(255, 184, 132);
        this.nameTextColor = new MongoColor(192, 209, 194);

        this.backgroundImage = "";
        this.outlineImage = "";
        this.xpEmptyImage = "";
        this.xpOutlineImage = "";
        this.xpFillImage = "";
        this.avatarOutlineImage = "";

        this.outlineOpacity = 1.0f;
    }

    public MongoColor getAvatarOutlineColor() {
        return this.avatarOutlineColor;
    }

    public String getAvatarOutlineImage() {
        return this.avatarOutlineImage;
    }

    public MongoColor getBackgroundColor() {
        return this.backgroundColor;
    }

    public String getBackgroundImage() {
        return this.backgroundImage;
    }

    public MongoColor getLevelTextColor() {
        return this.levelTextColor;
    }

    public MongoColor getNameTextColor() {
        return this.nameTextColor;
    }

    public MongoColor getOutlineColor() {
        return this.outlineColor;
    }

    public String getOutlineImage() {
        return this.outlineImage;
    }

    public float getOutlineOpacity() {
        return this.outlineOpacity;
    }

    public MongoColor getPercentTextColor() {
        return this.percentTextColor;
    }

    public MongoColor getRankTextColor() {
        return this.rankTextColor;
    }

    public MongoColor getXpEmptyColor() {
        return this.xpEmptyColor;
    }

    public String getXpEmptyImage() {
        return this.xpEmptyImage;
    }

    public MongoColor getXpFillColor() {
        return this.xpFillColor;
    }

    public String getXpFillImage() {
        return this.xpFillImage;
    }

    public MongoColor getXpOutlineColor() {
        return this.xpOutlineColor;
    }

    public String getXpOutlineImage() {
        return this.xpOutlineImage;
    }

    public MongoColor getXpTextColor() {
        return this.xpTextColor;
    }

    public void setAvatarOutlineColor(MongoColor avatarOutlineColor) {
        this.avatarOutlineColor = avatarOutlineColor;
    }

    public void setAvatarOutlineImage(String avatarOutlineImage) {
        this.avatarOutlineImage = avatarOutlineImage;
    }

    public void setBackgroundColor(MongoColor backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public void setBackgroundImage(String backgroundImage) {
        this.backgroundImage = backgroundImage;
    }

    public void setLevelTextColor(MongoColor levelTextColor) {
        this.levelTextColor = levelTextColor;
    }

    public void setNameTextColor(MongoColor nameTextColor) {
        this.nameTextColor = nameTextColor;
    }

    public void setOutlineColor(MongoColor outlineColor) {
        this.outlineColor = outlineColor;
    }

    public void setOutlineImage(String outlineImage) {
        this.outlineImage = outlineImage;
    }

    public void setOutlineOpacity(float outlineOpacity) {
        this.outlineOpacity = outlineOpacity;
    }

    public void setPercentTextColor(MongoColor percentTextColor) {
        this.percentTextColor = percentTextColor;
    }

    public void setRankTextColor(MongoColor rankTextColor) {
        this.rankTextColor = rankTextColor;
    }

    public void setXpEmptyColor(MongoColor xpEmptyColor) {
        this.xpEmptyColor = xpEmptyColor;
    }

    public void setXpEmptyImage(String xpEmptyImage) {
        this.xpEmptyImage = xpEmptyImage;
    }

    public void setXpFillColor(MongoColor xpFillColor) {
        this.xpFillColor = xpFillColor;
    }

    public void setXpFillImage(String xpFillImage) {
        this.xpFillImage = xpFillImage;
    }

    public void setXpOutlineColor(MongoColor xpOutlineColor) {
        this.xpOutlineColor = xpOutlineColor;
    }

    public void setXpOutlineImage(String xpOutlineImage) {
        this.xpOutlineImage = xpOutlineImage;
    }

    public void setXpTextColor(MongoColor xpTextColor) {
        this.xpTextColor = xpTextColor;
    }
}