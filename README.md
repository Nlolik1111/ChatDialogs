# ChatDialogs Advanced Dialog System

ChatDialogs ships as a single Gradle project that targets **Forge, Fabric, and Quilt** for Minecraft **1.20.x through 1.21.x** on both the client and the dedicated server. Shared logic lives inside `common/`, while loader-specific bootstrap modules reside in `forge/`, `fabric/`, and `quilt/`. Every section of this document is fully written in its own language so mod pack creators can share the guide with their communities without losing detail.

---

## English

### 1. Overview

* **Purpose** – orchestrate cinematic conversations, quest flows, automation routines, and cross-mod integrations without writing Java code.
* **Highlights**
  * Button-based dialog navigation with condition checks, per-button cooldowns, and one-click command execution (vanilla or third-party mods).
  * Scheduler for delays, loops, automatic node transitions, and deferred shutdowns through `stop_time`.
  * Event triggers (`on_player_join`, scoreboard checks, item use, custom hooks) that launch dialogs automatically.
  * Placeholder-aware text rendering with rich styling (`color`, `formats`, modifiers) for lines and buttons.

### 2. Directory Layout & Reloading

| Path | Description |
|------|-------------|
| `config/chatdialogs/dialogs/*.json` | Dialog definition files. Each file can hold one object, an array of dialogs, or an object with a top-level `dialogs` array. Drop as many files as you like; the loader indexes them all. |
| `/dialog reload` | Re-reads every JSON file without restarting the server on Forge, Fabric, or Quilt. |

* The mod normalises any mistakenly capitalised `.Json` files to `.json` so that auto-complete always sees them. A starter file `example.json` is generated the first time the folder is created.
* `/dialog start` suggestions list the **file name without the `.json` extension** (e.g., `test` for `test.json`). If multiple dialogs share the same file name, suggestions append the dialog id (`test:welcome`). Both aliases and ids work for `/dialog start`.

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

* `time_to_start`: countdown in seconds (floats allowed). Use `false` to start immediately.
* `stop_time`: keep the runtime alive for the remaining seconds so scheduled commands or data functions can finish cleanly.

### 4. Dialog Schema

| Field | Purpose |
|-------|---------|
| `id` | Unique dialog identifier. Defaults to the file name if omitted. |
| `name` | Human-readable title used in logs and chat feedback. |
| `start` | Starting node id (defaults to `start`). |
| `nodes` | Map of node id to node definition. |
| `events` | Optional list of event triggers that auto-start the dialog. |

**Node fields**

* `lines` / `messages` – array of strings or objects (`text`, `ticks`/`delay`, optional `loop` with `times`, `ticks`, replacement `text`).
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

Buttons appear inline when there are three or fewer options; otherwise they stack vertically. Hover text is localised (“Click to choose”, “Нажмите, чтобы выбрать”, etc.).

### 5. Actions & Placeholders

* Full command support (`command`, `button_execute`, `trigger_event`) including third-party mod commands.
* Placeholder engine covers `{player_name}`, `{player_health}`, `{player_coords.x}`, `{world_name}`, `{time}`, `{date}`, `{random:min:max}`, scoreboard shortcuts, and custom context values.
* Inventory, quest, and currency helpers (`give_item`, `take_item`, `check_inventory`, `complete_quest`, `reset_quest`, `give_currency`).
* World manipulation (`teleport`, `set_world_time`, `spawn_mob`, `place_block`, `remove_block`, `world_border`).
* Advanced flow control (`wait_until`, `loop`, `stop_time`, `random_check`, conditional branches).

### 6. Command Workflow

```
/dialog start <alias|id> [player]
/dialog stop [player]
/dialog choose <token>
/dialog reload
```

* Start suggestions display aliases derived from file names and fall back to dialog ids.
* Errors and confirmations are localised based on the player’s language.
* Buttons call `/dialog choose` automatically—keep the subcommand for scripts or manual testing.

### 7. Localization

* Chat messages and command feedback use translation keys in `assets/chatdialogs/lang/*.json`.
* Bundled languages: `en_us`, `ru_ru`, `uk_ua`, `es_es`.
* Add more languages by copying `en_us.json`, translating values, and saving the file as `assets/chatdialogs/lang/<language>.json` (e.g., `de_de.json`).
* The prefix `[Dialogs]` is translated per client so players can filter notifications easily.

### 8. Tips & Troubleshooting

* Combine `stop_time` with `wait_until` or `command` actions to guarantee that follow-up tasks finish before the runtime stops.
* Use `events` to launch dialogs from scoreboard objectives, item use, or custom triggers.
* If a dialog fails to load, the server log prints the JSON file path; fix the syntax and run `/dialog reload` again.
* Autocomplete skipping a file? Ensure the extension is lowercase `.json`; ChatDialogs will attempt to correct uppercase `.Json` files automatically.

---

## Русский

### 1. Обзор

* **Назначение** – создавать кинематографичные диалоги, квестовые сценарии, автоматизацию и интеграции с другими модами без Java-кода.
* **Ключевые возможности**
  * Кнопки с условиями, задержками и выполнением любых команд (ванильных или сторонних модов).
  * Планировщик задержек, циклов, автоматических переходов и отложенного завершения через `stop_time`.
  * События (`on_player_join`, проверки скорборда, использование предметов, пользовательские хуки), которые запускают диалоги автоматически.
  * Богатое форматирование текста с плейсхолдерами и стилями (`color`, `formats`, модификаторы) для реплик и кнопок.

### 2. Структура файлов и перезагрузка

| Путь | Описание |
|------|----------|
| `config/chatdialogs/dialogs/*.json` | Файлы диалогов. Можно хранить один объект, массив или объект с массивом `dialogs`. Добавляйте сколько угодно файлов — мод подхватит все. |
| `/dialog reload` | Перечитывает JSON без перезапуска сервера на Forge, Fabric и Quilt. |

* Мод автоматически переименовывает файлы с расширением `.Json` в `.json`, чтобы автодополнение всегда находило диалоги. При первом запуске создаётся пример `example.json`.
* В `/dialog start` подсказках отображается **имя файла без расширения `.json`** (например, `test` для `test.json`). Если несколько диалогов используют одно имя файла, добавляется `test:welcome`. Можно вводить как псевдоним, так и оригинальный `id`.

### 3. Быстрый пример

*(JSON из английского раздела полностью совместим — переведите строки по необходимости.)*

* `time_to_start` — задержка в секундах (`false` отключает отсчёт).
* `stop_time` — удерживает диалог активным, пока не истечёт время, даже если реплики закончились. Полезно для ожидания команд или функций данных.

### 4. Структура диалога

| Поле | Назначение |
|------|------------|
| `id` | Уникальный идентификатор (по умолчанию — имя файла). |
| `name` | Название для логов и сообщений. |
| `start` | Стартовый узел (по умолчанию `start`). |
| `nodes` | Словарь «узел → описание». |
| `events` | Необязательные триггеры запуска. |

**Узел может включать**

* `lines` / `messages` — строки или объекты с полями `text`, `ticks`/`delay`, `loop`.
* `actions` — действия при входе в узел.
* `buttons` — кнопки выбора. Условия проверяются при отображении и при нажатии, повторные нажатия запрещены.
* `branches` — последовательность `if` / `elif` / `else` с действиями и переходами.
* `auto_next` и `auto_next_delay` — автоматический переход в другой узел.
* `close` — закрыть диалог после завершения реплик, если нет `stop_time`.
* `stop_time` / `stop_ticks` — задержка перед завершением даже без кнопок и реплик.

**Поля кнопки**

| Поле | Описание |
|------|----------|
| `id` | Логический идентификатор кнопки. |
| `text` | Текст кнопки с поддержкой плейсхолдеров. |
| `style` | Объект со свойствами `color`, `formats`, `modifier`; поддерживаются также `button_color`, `button_format`, `button_modifier`. |
| `actions` | Выполняются при нажатии (команды, обновления квестов и т. д.). |
| `next` | Узел для перехода. |
| `conditions` / `if` | Условия отображения и клика. |
| `delay` | Задержка перед выполнением действий (в тиках). |
| `close` | Планирует завершение диалога (учитывается `stop_time`). |

До трёх кнопок выводятся в одну строку, больше — столбцом. Подсказка при наведении локализована.

### 5. Действия и плейсхолдеры

* Полная поддержка команд (`command`, `button_execute`, `trigger_event`) включая команды сторонних модов.
* Плейсхолдеры: `{player_name}`, `{player_health}`, координаты игрока, `{world_name}`, `{time}`, `{date}`, `{random:min:max}`, сокращения скорборда и пользовательские данные.
* Работа с инвентарём и прогрессом (`give_item`, `take_item`, `check_inventory`, `complete_quest`, `reset_quest`, `give_currency`).
* Управление миром (`teleport`, `set_world_time`, `spawn_mob`, `place_block`, `remove_block`, `world_border`).
* Расширенное управление логикой (`wait_until`, `loop`, `stop_time`, `random_check`, ветвления условий).

### 6. Команды

```
/dialog start <псевдоним|id> [игрок]
/dialog stop [игрок]
/dialog choose <token>
/dialog reload
```

* Подсказки используют имена файлов и `id`.
* Все сообщения об ошибках и успехе локализованы.
* Кнопки автоматически вызывают `/dialog choose`, но команда остаётся для скриптов и отладки.

### 7. Локализация

* Переводы находятся в `assets/chatdialogs/lang/*.json`.
* В комплект входят `en_us`, `ru_ru`, `uk_ua`, `es_es`.
* Чтобы добавить язык, скопируйте `en_us.json`, переведите значения и сохраните под нужным именем (`de_de.json` и т. п.).
* Префикс `[Диалоги]` отображается на языке клиента.

### 8. Советы и устранение неполадок

* Сочетайте `stop_time` с `wait_until` или `command`, чтобы дождаться завершения цепочек действий.
* `events` позволяют запускать диалоги при входе игрока, взаимодействиях, гибели мобов и других событиях.
* При ошибке загрузки путь к проблемному файлу выводится в лог сервера — исправьте JSON и выполните `/dialog reload`.
* Если файл не появляется в списке, убедитесь, что расширение — `.json`. ChatDialogs сам переименует `.Json`, но лог всё равно предупредит.

---

## Українська

### 1. Огляд

* **Призначення** – створювати кінематографічні діалоги, квестові сценарії, автоматизацію та інтеграції з іншими модами без Java-коду.
* **Ключові можливості**
  * Кнопки з умовами, затримками та виконанням будь-яких команд (ванільних і сторонніх модів).
  * Планувальник затримок, циклів, автоматичних переходів і відкладеного завершення через `stop_time`.
  * Події (`on_player_join`, перевірки скорборду, використання предметів, власні тригери), які запускають діалоги автоматично.
  * Багате форматування тексту з плейсхолдерами та стилями (`color`, `formats`, модифікатори) для реплік і кнопок.

### 2. Структура файлів і перезавантаження

| Шлях | Опис |
|------|------|
| `config/chatdialogs/dialogs/*.json` | Файли діалогів. Допускається один об'єкт, масив або об'єкт із масивом `dialogs`. Додавайте скільки завгодно файлів — мод завантажить усі. |
| `/dialog reload` | Перечитує JSON без перезапуску сервера на Forge, Fabric і Quilt. |

* Мод автоматично перейменовує файли з розширенням `.Json` на `.json`, щоб автодоповнення їх бачило. При першому запуску створюється приклад `example.json`.
* У підказках `/dialog start` показується **ім'я файлу без `.json`** (наприклад, `test`). Якщо кілька діалогів мають однакове ім'я файлу, додається `test:welcome`. Можна вводити псевдонім або справжній `id`.

### 3. Швидкий приклад

*(JSON з англійського розділу повністю підходить — перекладіть рядки за потреби.)*

* `time_to_start` — затримка у секундах (`false` вимикає відлік).
* `stop_time` — тримає діалог активним, доки не мине час, навіть якщо репліки завершилися. Зручно для очікування команд або data-функцій.

### 4. Структура діалогу

| Поле | Призначення |
|------|-------------|
| `id` | Унікальний ідентифікатор (типово — ім'я файлу). |
| `name` | Назва для логів і повідомлень. |
| `start` | Стартовий вузол (типово `start`). |
| `nodes` | Словник «вузол → опис». |
| `events` | Необов'язкові тригери запуску. |

**Вузол може містити**

* `lines` / `messages` — рядки або об'єкти з полями `text`, `ticks`/`delay`, `loop`.
* `actions` — дії під час входу у вузол.
* `buttons` — кнопки вибору. Умови перевіряються під час показу та під час натискання, повторні натискання заборонені.
* `branches` — послідовність `if` / `elif` / `else` з діями та переходами.
* `auto_next` і `auto_next_delay` — автоматичний перехід до іншого вузла.
* `close` — закрити діалог після завершення реплік, якщо немає `stop_time`.
* `stop_time` / `stop_ticks` — затримка перед завершенням навіть без кнопок.

**Поля кнопки**

| Поле | Опис |
|------|------|
| `id` | Логічний ідентифікатор кнопки. |
| `text` | Текст із підтримкою плейсхолдерів. |
| `style` | Об'єкт зі значеннями `color`, `formats`, `modifier`; також працюють `button_color`, `button_format`, `button_modifier`. |
| `actions` | Виконуються при натисканні (команди, оновлення квестів тощо). |
| `next` | Наступний вузол. |
| `conditions` / `if` | Умови показу та натискання. |
| `delay` | Затримка перед виконанням дій (у тіках). |
| `close` | Планує завершення діалогу (дотримується `stop_time`). |

До трьох кнопок відображаються в рядок, більше — стовпчиком. Підказка при наведенні локалізована.

### 5. Дії та плейсхолдери

* Повна підтримка команд (`command`, `button_execute`, `trigger_event`) разом із командами сторонніх модів.
* Плейсхолдери: `{player_name}`, `{player_health}`, координати гравця, `{world_name}`, `{time}`, `{date}`, `{random:min:max}`, скорочення скорбордів, додаткові дані контексту.
* Робота з інвентарем і прогресом (`give_item`, `take_item`, `check_inventory`, `complete_quest`, `reset_quest`, `give_currency`).
* Керування світом (`teleport`, `set_world_time`, `spawn_mob`, `place_block`, `remove_block`, `world_border`).
* Розширене керування логікою (`wait_until`, `loop`, `stop_time`, `random_check`, умовні гілки).

### 6. Команди

```
/dialog start <псевдонім|id> [гравець]
/dialog stop [гравець]
/dialog choose <token>
/dialog reload
```

* Підказки використовують імена файлів та `id`.
* Повідомлення про помилки та успіх локалізовані.
* Кнопки автоматично викликають `/dialog choose`, залишаючи команду для скриптів і діагностики.

### 7. Локалізація

* Файли перекладів розташовані в `assets/chatdialogs/lang/*.json`.
* У комплекті `en_us`, `ru_ru`, `uk_ua`, `es_es`.
* Щоб додати нову мову, скопіюйте `en_us.json`, перекладіть і збережіть під потрібною назвою.
* Префікс `[Діалоги]` відображається мовою клієнта.

### 8. Поради та вирішення проблем

* Поєднуйте `stop_time` з `wait_until` чи `command`, щоб дочекаватися завершення дій.
* `events` дозволяють запускати діалоги під час входу гравця, взаємодій, смерті мобів тощо.
* Якщо діалог не завантажується, у логах з'явиться шлях до файла — виправте JSON і запустіть `/dialog reload`.
* Файл не з'явився у списку? Перевірте, що розширення `.json`. ChatDialogs перейменує `.Json`, але краще виправити вручну.

---

## Español

### 1. Descripción general

* **Objetivo** – crear diálogos cinemáticos, flujos de misiones, automatizaciones e integraciones con otros mods sin escribir Java.
* **Características clave**
  * Botones con condiciones, retrasos y ejecución de cualquier comando (vanilla o de mods externos).
  * Planificador para retrasos, bucles, transiciones automáticas y cierre diferido mediante `stop_time`.
  * Eventos (`on_player_join`, comprobaciones de marcador, uso de objetos, ganchos personalizados) que inician diálogos automáticamente.
  * Texto con marcadores y estilo enriquecido (`color`, `formats`, modificadores) tanto para líneas como para botones.

### 2. Estructura de archivos y recarga

| Ruta | Descripción |
|------|-------------|
| `config/chatdialogs/dialogs/*.json` | Archivos de diálogo. Puede ser un objeto, una matriz de diálogos o un objeto con matriz `dialogs`. Añade tantos archivos como quieras; el mod los indexa todos. |
| `/dialog reload` | Vuelve a leer todos los JSON sin reiniciar el servidor en Forge, Fabric o Quilt. |

* El mod renombra automáticamente cualquier archivo con extensión `.Json` a `.json` para que el autocompletado los detecte. Al crear la carpeta por primera vez se genera `example.json`.
* Las sugerencias de `/dialog start` muestran el **nombre del archivo sin `.json`** (por ejemplo, `test`). Si varios diálogos comparten nombre de archivo, aparece `test:welcome`. Puedes usar el alias o el `id` real.

### 3. Ejemplo rápido

*(El JSON del apartado en inglés funciona sin cambios; traduce los textos si lo necesitas.)*

* `time_to_start` — retraso en segundos (`false` evita la cuenta regresiva).
* `stop_time` — mantiene el diálogo activo durante los segundos restantes para finalizar comandos o funciones de datos.

### 4. Esquema del diálogo

| Campo | Propósito |
|-------|-----------|
| `id` | Identificador único (por defecto, el nombre del archivo). |
| `name` | Nombre para registros y mensajes. |
| `start` | Nodo inicial (por defecto `start`). |
| `nodes` | Mapa de nodos con su definición. |
| `events` | Lista opcional de eventos que inician el diálogo. |

**Un nodo puede incluir**

* `lines` / `messages` — cadenas o objetos con `text`, `ticks`/`delay`, `loop`.
* `actions` — acciones al entrar al nodo.
* `buttons` — opciones interactivas. Las condiciones se revisan al mostrar y al pulsar; no se puede pulsar dos veces.
* `branches` — lista ordenada de `if` / `elif` / `else` con acciones y transiciones.
* `auto_next` y `auto_next_delay` — salto automático a otro nodo.
* `close` — cierra tras terminar las líneas si no hay `stop_time`.
* `stop_time` / `stop_ticks` — retrasa el cierre incluso sin más líneas o botones.

**Campos del botón**

| Campo | Descripción |
|-------|-------------|
| `id` | Identificador lógico para rastrear pulsaciones. |
| `text` | Texto con marcadores (por ejemplo `{player_name}`). |
| `style` | Objeto con `color`, `formats`, `modifier`; también se aceptan `button_color`, `button_format`, `button_modifier`. |
| `actions` | Acciones al pulsar (comandos, progreso de misiones, etc.). |
| `next` | Nodo al que se transiciona. |
| `conditions` / `if` | Condiciones para mostrar y pulsar. |
| `delay` | Retraso en ticks antes de ejecutar acciones. |
| `close` | Programa el cierre del diálogo (respeta `stop_time`). |

Hasta tres botones se muestran en una fila; con más se apilan en columna. El texto emergente está localizado.

### 5. Acciones y marcadores

* Soporte completo de comandos (`command`, `button_execute`, `trigger_event`) incluyendo mods externos.
* Marcadores: `{player_name}`, `{player_health}`, coordenadas del jugador, `{world_name}`, `{time}`, `{date}`, `{random:min:max}`, atajos de marcador y datos personalizados.
* Utilidades de inventario y progreso (`give_item`, `take_item`, `check_inventory`, `complete_quest`, `reset_quest`, `give_currency`).
* Manipulación del mundo (`teleport`, `set_world_time`, `spawn_mob`, `place_block`, `remove_block`, `world_border`).
* Control avanzado del flujo (`wait_until`, `loop`, `stop_time`, `random_check`, ramas condicionales).

### 6. Comandos

```
/dialog start <alias|id> [jugador]
/dialog stop [jugador]
/dialog choose <token>
/dialog reload
```

* Las sugerencias muestran alias basados en archivos y `id`.
* Los mensajes de error y confirmación se localizan según el idioma del jugador.
* Los botones ejecutan `/dialog choose` automáticamente; el subcomando permanece disponible para scripts y pruebas.

### 7. Localización

* Las traducciones residen en `assets/chatdialogs/lang/*.json`.
* Se incluyen `en_us`, `ru_ru`, `uk_ua`, `es_es`.
* Para añadir otro idioma, copia `en_us.json`, traduce los valores y guarda el archivo con el nombre adecuado (`de_de.json`, `pt_br.json`, etc.).
* El prefijo `[Diálogos]` se muestra en el idioma del cliente.

### 8. Consejos y resolución de problemas

* Combina `stop_time` con `wait_until` o acciones `command` para asegurarte de que las tareas terminen antes de cerrar el diálogo.
* Usa `events` para iniciar diálogos al entrar jugadores, interactuar con bloques, derrotar entidades y más.
* Si un diálogo no se carga, revisa los registros del servidor: verás la ruta del archivo con error. Corrige el JSON y ejecuta `/dialog reload`.
* ¿El autocompletado no muestra un archivo? Asegúrate de que la extensión es `.json`. ChatDialogs intenta corregir `.Json`, pero siempre es mejor renombrarlo manualmente.
