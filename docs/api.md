# API de raças

Toda raça é registrada no `RaceRegistry` (módulo `core`). A tela de seleção lista automaticamente tudo o que estiver registrado, na ordem de registro.

## Registrando uma raça

No `fabric.mod.json` do seu addon:

```json
"entrypoints": {
	"datura_origins": ["com.exemplo.addon.MinhasRacas"]
}
```

```java
public class MinhasRacas implements RaceProvider {
	public static Race ELFO;

	@Override
	public void registerRaces() {
		ELFO = RaceRegistry.register(Race.builder("meuaddon:elfo")
				.name("Elfo")
				.description("Guardiões antigos das florestas.")
				.advantage("Visão aguçada à noite")
				.disadvantage("Frágil contra fogo")
				.ability("Passo sombrio", "Teleporta alguns blocos à frente.")
				.passive("Regeneração lenta sob as árvores")
				.color(0x7FD6A0)
				.build());
	}
}
```

Textos podem ser literais ou chaves de tradução (`lang/*.json`).

## Raça por JSON (com poderes)

Coloque um arquivo em `datura_races/<id>.json` dentro do seu mod. A ordem na lista segue `order`.

```json
{
	"id": "elfo",
	"order": 10,
	"name": "Elfo",
	"color": "#7FD6A0",
	"description": "Guardiões antigos das florestas.",
	"advantages": ["Visão noturna permanente."],
	"disadvantages": ["Frágil contra fogo."],
	"ability": { "type": "datura_origins:fireball", "name": "Bola de Fogo", "description": "...", "cooldown": 300 },
	"powers": [ { "type": "datura_origins:status_effect", "effect": "minecraft:night_vision" } ]
}
```

Variantes da mesma raça usam `"variant_of": "<id da base>"`; elas aparecem na lista logo depois da raça base, como raças próprias.
Tipos novos de poder/habilidade: entrypoint `datura_origins:powers` (`DaturaOriginsPlugin`) chamando `PowerRegistry.registerPowerType`/`registerAbilityType`.

## Aparência na prévia (cliente)

```java
RacePreviews.register("meuaddon:elfo", (context, player, x, y, size, yaw, pitch, delta) -> {
	// desenhe o modelo da raça aqui
});
```

Sem registro, a prévia usa o próprio jogador.

## Consultar a origem de um jogador

`OriginData.get(player)` retorna `Optional<Race>`.
