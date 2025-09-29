# ChatDialogs Advanced Dialog System

The ChatDialogs dialog runtime for Forge 1.20.1 lets you build branching conversations, trigger Minecraft or mod commands, and react to in-game events. This guide is organised by language so you can jump straight to the section that matches your players. English comes first, followed by Russian and then additional languages.

---

## English

### 1. Overview

* **Purpose** – orchestrate cinematic conversations, quest flows, and automation routines without writing Java code.
* **Highlights**
  * Button-based dialog navigation with condition checks and one-click command execution.
  * Scheduler for delays, loops, automatic node transitions, and deferred shutdowns through `stop_time`.
  * Event triggers (`on_player_join`, custom scoreboard checks, etc.) that launch dialogs automatically.
  * Full command support: vanilla commands, third-party mod commands, and custom placeholder-aware text rendering.
  * Localised chat feedback that honours each client’s interface language (English, Russian, Ukrainian, Spanish are bundled by default).

### 2. File Layout & Reloading

| Path | Description |
|------|-------------|
| `config/ChatDialogs/dialogs/*.json` | Dialog definition files. Each file can hold one dialog object, an array of dialog objects, or an object with a top-level `dialogs` array. |
| `/dialog reload` | Re-reads all JSON files without restarting the server. |

When you run `/dialog start`, the auto-complete suggestions show the **file name without the `.json` extension** (for example, `test` for `test.json`). If multiple dialogs share the same file name, a suffix such as `test:welcome` keeps entries unique; both the alias and the real `id` are accepted when starting a dialog.

### 3. Quick Start Example

```jsonc
{
  "id": "welcome",
  "name": "Welcome Sequence",
  "start": "intro",
  "time_to_start": 5,
  "nodes": {
    "intro": {
      "lines": [
        "&6Hello, {player_name}!",
        { "text": "&7Current time: {time}", "ticks": 40 }
      ],
      "buttons": [
        {
          "id": "gift",
          "text": "&aClaim Gift",
          "actions": [
            { "type": "give_item", "item": "minecraft:diamond", "count": 3 },
            { "type": "send_message", "message": "&aEnjoy your reward!" }
          ],
          "next": "quest",
          "delay": 10
        },
        {
          "id": "later",
          "text": "&cMaybe Later",
          "actions": [ { "type": "send_message", "message": "&7Come back when you are ready." } ],
          "close": true
        }
      ]
    },
    "quest": {
      "lines": [
        { "text": "&eCheck your quest progress.", "ticks": 20 }
      ],
      "branches": [
        {
          "type": "if",
          "condition": "{score:quest_starter} >= 1",
          "actions": [ { "type": "send_message", "message": "&aQuest complete!" } ],
          "next": "reward"
        },
        {
          "type": "else",
          "actions": [ { "type": "send_message", "message": "&cYou still need to finish the quest." } ],
          "close": true
        }
      ]
    },
    "reward": {
      "actions": [
        { "type": "give_currency", "value": "50" },
        { "type": "complete_quest", "complete_quest": "starter" }
      ],
      "close": true,
      "stop_time": 3
    }
  }
}
```

* `time_to_start`: delay (seconds, floats allowed). Use `false` to skip the countdown.
* `stop_time`: keep the runtime alive for extra seconds after the last line or button to let data functions finish.

### 4. Dialog Schema

| Field | Purpose |
|-------|---------|
| `id` | Unique dialog identifier. Defaults to the file name if omitted. |
| `name` | Human-readable title used in logs and UI feedback. |
| `start` | Starting node id (defaults to `start`). |
| `nodes` | Map of node id to node definition. |
| `events` | Optional list of event triggers that auto-start the dialog. |

**Node fields**

* `lines` / `messages` – array of strings or objects (`text`, `ticks`/`delay`, optional `loop` with `times`, `ticks`, and replacement `text`).
* `actions` – executed once when the node starts.
* `buttons` – interactive choices. Buttons honour conditions twice (display + click) and cannot be pressed more than once.
* `branches` – ordered list of `if` / `elif` / `else` branches with optional `actions` and `next`.
* `auto_next` + `auto_next_delay` – jump to another node automatically.
* `close` – close immediately after all lines finish when no `stop_time` is defined.
* `stop_time` / `stop_ticks` – defer shutdown even if the node has no more lines or buttons.

**Button fields**

| Field | Description |
|-------|-------------|
| `id` | Logical id used for tracking presses. |
| `text` | Label with placeholder support (`{player_name}`, `{random:1:5}`, etc.). |
| `style` | Optional object with `color`, `formats`, `modifier`. Fallbacks include `button_color`, `button_format`, `button_modifier`. |
| `actions` | Executed when the button is pressed (commands, quest updates, etc.). |
| `next` | Next node id to enter. |
| `conditions` / `if` | Conditions that must be true to show and to click. |
| `delay` | Delay in ticks before running the actions. |
| `close` | Immediately schedule dialog shutdown (still respects `stop_time`). |

Buttons are rendered inline when there are three or fewer options; otherwise they appear in a column. Hover text is localised (“Click to choose”, “Нажмите, чтобы выбрать”, etc.).

### 5. Actions & Placeholders

Most actions come in pairs (`give_item`, `take_item`, `set_world_time`, `trigger_event`, etc.) and every string field runs through the placeholder engine. Some highlights:

* `{player_name}`, `{player_health}`, `{player_coords.x}`, `{time}`, `{date}`, `{world_name}`.
* Scoreboard shorthand `{score:<objective>}` for comparisons and actions.
* `{random:min:max}` for deterministic random numbers per execution.
* Inventory checks, quest completion (`complete_quest`, `reset_quest`), currency payout (`give_currency`).
* Spatial manipulation (`teleport`, `spawn_mob`, `world_border`, `place_block`, `remove_block`).
* Awaiting conditions with `wait_until` (polling interval + optional timeout).

A full action matrix is available inside the JSON parser; refer to this section when creating your own types.

### 6. Command Workflow

```
/dialog start <alias|id> [player]
/dialog choose <token>
/dialog stop [player]
/dialog reload
```

* Start suggestions show file-based aliases first (e.g., `test`).
* Syntax or lookup errors are now localised per player language.
* `/dialog choose` tokens are single-use; players will see a localised error message if they try to click twice or if conditions changed.


### 7. Tips for Automation

* Combine `stop_time` with `wait_until` or `command` actions to guarantee that follow-up data functions finish before the runtime stops.
* Use `events` to launch dialogs from scoreboard objectives, item use, or custom triggers.
* Remember that button actions can include `close: true`—the runtime still honours any pending `stop_time` delay before actually shutting down.

---

## Русский

### 1. Обзор

* **Назначение** – создавать кинематографичные диалоги, квестовые сценарии и автоматизацию без Java-кода.
* **Ключевые возможности**
  * Кнопки с условиями и выполнением команд (в том числе сторонних модов).
  * Планировщик задержек, циклов и автоматических переходов, поддержка `stop_time` для отсрочки завершения.
  * События (`on_player_join` и т. д.), которые запускают диалог автоматически.
  * Полная локализация сообщений – игроки видят уведомления на языке клиента (английский, русский, украинский, испанский включены).

### 2. Структура файлов и перезагрузка

| Путь | Описание |
|------|----------|
| `config/ChatDialogs/dialogs/*.json` | Файлы диалогов. Допускается один объект, массив объектов или объект с массивом `dialogs`. |
| `/dialog reload` | Перечитывает JSON без перезапуска сервера. |

В автодополнении команды `/dialog start` показываются **имена файлов без расширения `.json`**. Для `test.json` будет предложено `test`. Если несколько диалогов делят одно имя файла, появится вариант `test:<id>`; можно вводить как этот псевдоним, так и оригинальный `id`.

### 3. Быстрый пример

*(англоязычный JSON выше остаётся валидным; можно перевести строки и плейсхолдеры по необходимости.)*

* `time_to_start` — задержка в секундах, `false` отключает обратный отсчёт.
* `stop_time` — удерживает диалог активным оставшееся время, даже если реплики закончились. Это удобно для ожидания сторонних команд или функций данных.

### 4. Структура диалога

| Поле | Назначение |
|------|------------|
| `id` | Уникальный идентификатор (по умолчанию — имя файла). |
| `name` | Название для логов и сообщений. |
| `start` | Стартовый узел (по умолчанию `start`). |
| `nodes` | Словарь «узел → описание». |
| `events` | Необязательные триггеры запуска. |

**Узел** может содержать `lines` / `messages`, `actions`, `buttons`, `branches`, `auto_next`, `stop_time` и др. Условия кнопок проверяются дважды, кнопки нельзя нажимать повторно, кнопки с `close: true` всё равно учитывают задержку `stop_time`.

### 5. Действия и плейсхолдеры

* Основные плейсхолдеры: `{player_name}`, `{player_health}`, `{player_coords.x}`, `{time}`, `{date}`, `{world_name}`, `{random:min:max}`.
* Работа со скорбордами, выдачей и изъятием предметов, телепортацией, мобами, границами мира, валютой и квестами.
* `wait_until` позволяет ожидать условие с интервалом и тайм-аутом.
* Любая строка проходит через движок плейсхолдеров и поддержку форматирования (`&`-коды, объект `style`).

### 6. Команды

```
/dialog start <псевдоним|id> [игрок]
/dialog choose <token>
/dialog stop [игрок]
/dialog reload
```

* Все сообщения об ошибках и успехе локализованы (с учётом языка клиента).
* Кнопки удаляются после первого нажатия, повторная попытка сообщает об ошибке.

### 7. Советы по автоматизации

* Для безопасного выполнения цепочек команд объединяйте `stop_time` с действиями `wait_until` или `command`.
* События позволяют запускать диалоги при входе игрока, использовании предметов или изменении скорборда.
* Помните, что `stop_time` не даёт диалогу завершиться, пока не истечёт указанное время.

----
Available languages: English, Russian, Ukraine, Spanish


