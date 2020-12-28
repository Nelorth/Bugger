package tech.bugger.global.transfer;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

/**
 * DTO representing an organization.
 */
public class Organization implements Serializable {

    @Serial
    private static final long serialVersionUID = 2775773770560723398L;

    /**
     * Organization name.
     */
    private String name;

    /**
     * Organization logo.
     */
    private byte[] logo;

    /**
     * Organization theme.
     */
    private String theme;

    /**
     * Organization imprint.
     */
    private String imprint;

    /**
     * Organization privacy policy.
     */
    private String privacyPolicy;

    /**
     * Constructs a new organization from the specified parameters.
     *
     * @param name          The organization name.
     * @param logo          The organization logo.
     * @param theme         The organization theme.
     * @param imprint       The organization imprint.
     * @param privacyPolicy The organization privacy policy.
     */
    public Organization(final String name, final byte[] logo, final String theme,
                        final String imprint, final String privacyPolicy) {
        this.name = name;
        this.logo = logo;
        this.theme = theme;
        this.imprint = imprint;
        this.privacyPolicy = privacyPolicy;
    }

    /**
     * Returns the name of this organization.
     *
     * @return The organization name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of this organization.
     *
     * @param name The organization name to be set.
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Returns the logo of this organization.
     *
     * @return The organization logo.
     */
    public byte[] getLogo() {
        return logo;
    }

    /**
     * Returns the logo of this organization.
     *
     * @param logo The organization logo to be set.
     */
    public void setLogo(final byte[] logo) {
        this.logo = logo;
    }

    /**
     * Returns the theme of this organization.
     *
     * @return The organization theme.
     */
    public String getTheme() {
        return theme;
    }

    /**
     * Sets the theme of this organization.
     *
     * @param theme The organization theme to be set.
     */
    public void setTheme(final String theme) {
        this.theme = theme;
    }

    /**
     * Returns the imprint of the organization.
     *
     * @return The organization imprint.
     */
    public String getImprint() {
        return imprint;
    }

    /**
     * Sets the imprint of the organization.
     *
     * @param imprint The organization imprint to be set.
     */
    public void setImprint(final String imprint) {
        this.imprint = imprint;
    }

    /**
     * Returns the privacy policy of the organization.
     *
     * @return The organization privacy policy.
     */
    public String getPrivacyPolicy() {
        return privacyPolicy;
    }

    /**
     * Sets the privacy policy of the organization.
     *
     * @param privacyPolicy The organization privacy policy to be set.
     */
    public void setPrivacyPolicy(final String privacyPolicy) {
        this.privacyPolicy = privacyPolicy;
    }

    /**
     * Indicates whether some {@code other} organization is semantically equal to this organization.
     *
     * @param other The object to compare this organization to.
     * @return {@code true} iff {@code other} is a semantically equivalent organization.
     */
    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Organization)) {
            return false;
        }
        Organization that = (Organization) other;
        return name.equals(that.name)
                && Arrays.equals(logo, that.logo)
                && theme.equals(that.theme)
                && imprint.equals(that.imprint)
                && privacyPolicy.equals(that.privacyPolicy);
    }

    /**
     * Calculates a hash code for this organization for hashing purposes, and to fulfil the {@link
     * Object#equals(Object)} contract.
     *
     * @return The hash code value of this organization.
     */
    @Override
    public int hashCode() {
        int result = Objects.hash(name, theme, imprint, privacyPolicy);
        result = 31 * result + Arrays.hashCode(logo);
        return result;
    }

    /**
     * Converts this organization into a human-readable string representation.
     *
     * @return A human-readable string representation of this organization.
     */
    @Override
    public String toString() {
        return "Organization{"
                + "name='" + name + '\''
                + ", logo=" + Arrays.toString(logo)
                + ", theme='" + theme + '\''
                + ", imprint='" + imprint + '\''
                + ", privacyPolicy='" + privacyPolicy + '\''
                + '}';
    }

}
