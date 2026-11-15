package com.datura.origins.race;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Race {
	private final String id;
	private final String name;
	private final String description;
	private final List<String> advantages;
	private final List<String> disadvantages;
	private final List<String> passives;
	private final String abilityName;
	private final String ability;
	private final String variantOf;
	private final String variantName;
	private final List<String> models;
	private final int color;

	private Race(Builder builder) {
		this.id = builder.id;
		this.name = builder.name;
		this.description = builder.description;
		this.advantages = List.copyOf(builder.advantages);
		this.disadvantages = List.copyOf(builder.disadvantages);
		this.passives = List.copyOf(builder.passives);
		this.abilityName = builder.abilityName;
		this.ability = builder.ability;
		this.variantOf = builder.variantOf;
		this.variantName = builder.variantName;
		this.models = List.copyOf(builder.models);
		this.color = builder.color;
	}

	public static Builder builder(String id) {
		return new Builder(id);
	}

	public String id() {
		return id;
	}

	public String name() {
		return name;
	}

	public String description() {
		return description;
	}

	public List<String> advantages() {
		return advantages;
	}

	public List<String> disadvantages() {
		return disadvantages;
	}

	public List<String> passives() {
		return passives;
	}

	public String abilityName() {
		return abilityName;
	}

	public String ability() {
		return ability;
	}

	public String variantOf() {
		return variantOf;
	}

	public String variantName() {
		return variantName;
	}

	public boolean isVariant() {
		return variantOf != null;
	}

	public List<String> models() {
		return models;
	}

	public int color() {
		return color;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof Race race && race.id.equals(id);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public String toString() {
		return "Race[" + id + "]";
	}

	public static final class Builder {
		private final String id;
		private String name;
		private String description = "";
		private final List<String> advantages = new ArrayList<>();
		private final List<String> disadvantages = new ArrayList<>();
		private final List<String> passives = new ArrayList<>();
		private String abilityName;
		private String ability;
		private String variantOf;
		private String variantName;
		private final List<String> models = new ArrayList<>();
		private int color = 0xC9A75A;

		private Builder(String id) {
			if (id == null || id.indexOf(':') <= 0) {
				throw new IllegalArgumentException("Race id must be namespaced (modid:name): " + id);
			}
			this.id = id;
			this.name = id.substring(id.indexOf(':') + 1);
		}

		public Builder name(String name) {
			this.name = Objects.requireNonNull(name);
			return this;
		}

		public Builder description(String description) {
			this.description = Objects.requireNonNull(description);
			return this;
		}

		public Builder advantage(String text) {
			advantages.add(Objects.requireNonNull(text));
			return this;
		}

		public Builder disadvantage(String text) {
			disadvantages.add(Objects.requireNonNull(text));
			return this;
		}

		public Builder passive(String text) {
			passives.add(Objects.requireNonNull(text));
			return this;
		}

		public Builder ability(String name, String description) {
			this.abilityName = name;
			this.ability = description;
			return this;
		}

		public Builder variantOf(String baseId, String label) {
			this.variantOf = baseId;
			this.variantName = label;
			return this;
		}

		public Builder variantName(String label) {
			this.variantName = label;
			return this;
		}

		public Builder model(String model) {
			if (model != null && !model.isEmpty()) models.add(model);
			return this;
		}

		public Builder color(int rgb) {
			this.color = rgb & 0xFFFFFF;
			return this;
		}

		public Race build() {
			return new Race(this);
		}
	}
}
