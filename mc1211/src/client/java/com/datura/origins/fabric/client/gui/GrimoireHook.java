package com.datura.origins.fabric.client.gui;

/**
 * Gancho reservado para um processo futuro na tela do grimório.
 * <p>
 * Hoje nada é feito aqui: a implementação padrão ({@link #NONE}) não altera o comportamento da tela.
 * Para usar, implemente esta interface e instale com {@link GrimoireHook#install(GrimoireHook)}
 * no inicializador do cliente.
 */
public interface GrimoireHook {
	GrimoireHook NONE = new GrimoireHook() {
	};

	/**
	 * Chamado uma vez, no primeiro quadro em que o livro aparece sozinho na tela.
	 */
	default void onBookShown(IntroScreen screen) {
	}

	/**
	 * Chamado quando o jogador clica no livro, antes da abertura.
	 * Retornar {@code false} segura a abertura (por exemplo, para esperar outro processo terminar).
	 */
	default boolean beforeOpen(IntroScreen screen) {
		return true;
	}

	/**
	 * Chamado quando a animação de abertura termina, logo antes de ir para a escolha de origem.
	 */
	default void onOpened(IntroScreen screen) {
	}

	static void install(GrimoireHook hook) {
		Holder.current = hook == null ? NONE : hook;
	}

	static GrimoireHook current() {
		return Holder.current;
	}

	final class Holder {
		private static GrimoireHook current = NONE;

		private Holder() {
		}
	}
}
