package dev.darealturtywurty.superturtybot.commands.fun.relationship;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.*;

@Getter
@EqualsAndHashCode
@ToString
public class Person {
    private final long id;
    private final Set<Partner> partners = new HashSet<>();
    private Person parent1, parent2;
    private final Set<Person> children = new HashSet<>();
    private boolean isDead = false;

    public Person(long id) {
        this.id = id;
    }

    public boolean isSingle() {
        return this.partners.stream().allMatch(partner -> partner.getStatus() == Partner.PartnerStatus.SINGLE);
    }

    public void setPartner(Partner partner, Partner.PartnerStatus status) {
        if (this.partners.contains(partner)) {
            partner.setStatus(status);
            return;
        }

        this.partners.add(partner);
        partner.setStatus(status);
    }

    public void setParents(Person parent1, Person parent2) {
        this.parent1 = parent1;
        this.parent2 = parent2;

        if (parent1 != null) {
            parent1.getChildren().add(this);
        }

        if (parent2 != null) {
            parent2.getChildren().add(this);
        }
    }

    public void disownChild(Person child) {
        this.children.remove(child);
        child.onParentDisown(this);
    }

    public void onParentDisown(Person parent) {
        if (Objects.equals(this.parent1, parent)) {
            this.parent1 = null;
            getSiblings().forEach(sibling -> sibling.getSiblings().remove(this));
        } else if (Objects.equals(this.parent2, parent)) {
            this.parent2 = null;
            getSiblings().forEach(sibling -> sibling.getSiblings().remove(this));
        }
    }

    public void die() {
        this.isDead = true;

        if (!isSingle()) {
            this.partners.forEach(partner -> updatePartnerStatus(partner, Partner.PartnerStatus.DEAD));
        }
    }

    public boolean updatePartnerStatus(Partner partner, Partner.PartnerStatus status) {
        if (this.partners.contains(partner) || !canBePartneredWith(partner.getPerson()))
            return false;

        if (partner.getPerson() != null) {
            partner.setStatus(status);
            partner.getPerson().getPartners().stream().filter(p -> p.getPerson() == this)
                    .findFirst()
                    .ifPresent(p -> p.setStatus(status));
            return true;
        }

        return false;
    }

    public List<Person> getSiblings() {
        List<Person> siblings = new ArrayList<>();
        if (this.parent1 != null) {
            siblings.addAll(this.parent1.getChildren());
        }

        if (this.parent2 != null) {
            siblings.addAll(this.parent2.getChildren());
        }

        siblings.remove(this);
        return siblings;
    }

    public boolean haveChild(Person child, Partner partner) {
        if (this.isDead)
            return false;

        if (!this.partners.contains(partner))
            return false;

        this.children.add(child);
        child.setParents(this, partner.getPerson());

        if (partner.getPerson() != null) {
            partner.getPerson().getChildren().add(child);
        }

        return true;
    }

    public boolean canBePartneredWith(Person person) {
        // If either of the people are dead, they can't be partnered
        if (this.isDead || person.isDead)
            return false;

        // If the people are the same, they can't be partnered
        if (this == person)
            return false;

        // If the person is already partnered with the other person, they can't be partnered
        if (this.partners.stream().anyMatch(partner -> partner.getPerson() == person))
            return false;

        // If the person is a direct ancestor, they can't be partnered
        if (isAncestorOf(person))
            return false;

        // If the person is a direct descendant, they can't be partnered
        if (person.isAncestorOf(this))
            return false;

        // If the person is a sibling, they can't be partnered
        if (getSiblings().contains(person))
            return false;

        // If the person is a nibling, they can't be partnered
        if (isNibling(person))
            return false;

        return true;
    }

    public boolean isAncestorOf(Person person) {
        if (this.children.contains(person))
            return true;

        for (Person child : this.children) {
            if (child.isAncestorOf(person))
                return true;
        }

        return false;
    }

    public String getRelationshipTo(Person person) {
        if (Objects.equals(this, person))
            return "You";

        if (isAncestorOf(person))
            return "Parent";

        if (person.isAncestorOf(this))
            return "Child";

        if (getSiblings().contains(person))
            return "Sibling";

        if (isNibling(person))
            return "Nibling";

        if(this.partners.stream().anyMatch(partner -> partner.getPerson() == person))
            return switch (this.partners.stream().filter(partner -> partner.getPerson() == person).findFirst().get().getStatus()) {
                case MARRIED -> "Spouse";
                case ENGAGED -> "Fiance";
                case DATING -> "Boyfriend/Girlfriend";
                case DEAD -> "Widow/Widower";
                case DIVORCED -> "Ex-Spouse";
                case FRIENDS_WITH_BENEFITS -> "Friend with Benefits";
                case SINGLE -> "Single";
            };

        return "Stranger";
    }

    public boolean isNibling(Person person) {
        return getSiblings().contains(person.getParent1()) || getSiblings().contains(person.getParent2())
                || getSiblings().stream().anyMatch(sibling -> sibling.getChildren().contains(person));
    }

    public Set<Person> getConnections() {
        Set<Person> connections = new HashSet<>();
        connections.addAll(this.children);
        connections.addAll(this.partners.stream().map(Partner::getPerson).toList());
        connections.add(this.parent1);
        connections.add(this.parent2);
        return connections;
    }

    @Data
    public static class Partner {
        private Person person;
        private PartnerStatus status;

        public Partner(Person person, PartnerStatus status) {
            this.person = person;
            this.status = status;
        }

        public enum PartnerStatus {
            MARRIED, ENGAGED, DATING, DEAD, DIVORCED, FRIENDS_WITH_BENEFITS, SINGLE
        }
    }
}
