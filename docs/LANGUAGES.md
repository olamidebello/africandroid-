# African languages

The bundled offline searchable directory contains **2379 language records and 4208 dialect records** from Glottolog 5.3, filtered to records whose Macroarea contains Africa. Families are excluded. Glottocodes preserve identities even when an ISO-639-3 code is absent. Names are source names, not verified native autonyms.

This is a geographic catalog rather than a definitive classification of nativeness. Extinct varieties and sign languages may be included. Languages without an Africa macroarea assignment and African diaspora varieties outside that macroarea need manual review. Do not describe the directory as every known African native language or as fully localized Android.

## What works in the customization app

Open African language directory; search names, ISO codes or Glottocodes offline. Selecting a record shows its ID and explicit unverified support status, then offers Android language settings. Only locales actually supplied by the installed Android build can be selected there. Catalog membership does not add an Android system locale or menu translations.

## Implementing complete support

For each language, collect community-reviewed native names, orthography, script, locale/region codes and translations. Track framework/SystemUI/Settings/app translations separately. Add fonts only after coverage and rendering checks, including Ethiopic, Arabic/Ajami, N'Ko, Tifinagh, Vai, Bamum and other scripts as applicable. Add and test input methods; RTL, combining marks and text shaping need device tests. Speech recognition and synthesis need separately licensed models, data and performance testing. Do not make keyboard or speech claims from a catalog entry.

Maintain reviewed locale overlays and translations in separate language packs. Run completeness checks, native-speaker review and emulator/device tests before moving a record from unverified to supported. Languages and dialect classifications evolve; preserve stable IDs and review changes between catalog releases.

## Attribution and reproduction

Glottolog 5.3, edited by Hammarström, Harald; Forkel, Robert; Haspelmath, Martin; and Bank, Sebastian. https://glottolog.org/ and https://github.com/glottolog/glottolog-cldf/tree/v5.3 . Source data is CC BY 4.0: https://creativecommons.org/licenses/by/4.0/ . The project transforms source records into JSON shards and adds support-status fields. Source SHA-256 is stored in the bundled index.

Regenerate with `python3 scripts/import-languages.py /path/to/glottolog-v5.3-languages.csv`. Obtain that file from the pinned v5.3 source URL and verify its hash against the index before regenerating. This operation updates JSON shards; review upstream version changes before accepting them.
