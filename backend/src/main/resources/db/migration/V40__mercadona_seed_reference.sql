-- The 23 seeded Mercadona products, matched to the shop's own catalogue (FOR-198).
--
-- Additive on top of V39 (ADR-003). V36 transcribed these rows from docs/fitness_os.xlsm, so they
-- carried a name somebody typed and a price from whenever that sheet was written: no shop id, no
-- photo, and therefore no way to refresh them. This migration gives them both, and takes the
-- occasion to replace the transcribed figures with what Mercadona lists today.
--
-- HOW THE MATCHES WERE MADE: their catalogue was crawled once (26 categories, 151 subcategories,
-- 4,620 products -- there is no search endpoint, see MercadonaCatalogAdapter) and each seeded name
-- was matched against it by hand. Two rows are deliberately left untouched because the shop does
-- not sell them: 'mercadona-whey-protein' (V22 already noted "no suele ser compra Mercadona") and
-- 'mercadona-sweet-potato' (no boniato listed). Inventing a link for those would point a row at a
-- product nobody chose, and the screen offers no refresh where there is no source -- which is the
-- honest state.
--
-- WHY EVERY UPDATE IS SCOPED: `external_id IS NULL AND name = <the V36 value>` means a row an admin
-- has already edited, imported or refreshed is skipped. The seeded values are the only thing this
-- migration knows how to recognise, so they are the only thing it is willing to overwrite -- the
-- same rule V23 used when it removed the seeded shopping products.
--
-- IMAGES ARE STORED ALREADY CROPPED to 24x24 through imgix's own query parameters, the size the
-- catalog list draws them at. Callers still resize on the way out (thumbnailUrl), so the import
-- dialog gets its 40 px version; storing the small one just means the common case downloads a
-- thumbnail instead of a 300 px photo.

UPDATE store_product SET
    external_id = '86341', image_url = 'https://prod-mercadona.imgix.net/images/98910a7833008eddea7deffc843ffac7.jpg?fit=crop&h=24&w=24',
    name = 'Copos de avena Brüggen', package_size = 'Caja 0.8 kg', price_eur = 1.30,
    url = 'https://tienda.mercadona.es/product/86341'
  WHERE id = 'mercadona-oats' AND external_id IS NULL AND name = 'Copos de avena Brüggen';

UPDATE store_product SET
    external_id = '3819', image_url = 'https://prod-mercadona.imgix.net/images/e4a37940916985bf5ca166e266580c37.jpg?fit=crop&h=24&w=24',
    name = 'Plátano de Canarias IGP', package_size = 'Pieza 0.17 kg', price_eur = 0.39,
    url = 'https://tienda.mercadona.es/product/3819'
  WHERE id = 'mercadona-banana' AND external_id IS NULL AND name = 'Plátanos';

UPDATE store_product SET
    external_id = '31504', image_url = 'https://prod-mercadona.imgix.net/images/707625e3e76b36f1834d56c1ed8aa8df.jpg?fit=crop&h=24&w=24',
    name = 'Huevos grandes L', package_size = 'Paquete 12 ud', price_eur = 3.05,
    url = 'https://tienda.mercadona.es/product/31504'
  WHERE id = 'mercadona-eggs' AND external_id IS NULL AND name = 'Huevos grandes L';

UPDATE store_product SET
    external_id = '31312', image_url = 'https://prod-mercadona.imgix.net/images/cde7e603fa71c589abe8e10279bd36ab.jpg?fit=crop&h=24&w=24',
    name = 'Claras de huevo líquidas pasteurizadas', package_size = 'Botella 1 l', price_eur = 2.85,
    url = 'https://tienda.mercadona.es/product/31312'
  WHERE id = 'mercadona-egg-whites' AND external_id IS NULL AND name = 'Claras de huevo líquidas pasteurizadas';

UPDATE store_product SET
    external_id = '51071', image_url = 'https://prod-mercadona.imgix.net/images/d2f12f4f6b2de080b5aeebc8aa1a5e46.jpg?fit=crop&h=24&w=24',
    name = 'Queso fresco batido desnatado 0% MG Hacendado', package_size = 'Tarrina 0.5 kg', price_eur = 1.10,
    url = 'https://tienda.mercadona.es/product/51071'
  WHERE id = 'mercadona-fresh-cheese' AND external_id IS NULL AND name = 'Queso fresco batido 0% MG Hacendado';

UPDATE store_product SET
    external_id = '21256', image_url = 'https://prod-mercadona.imgix.net/images/7ad06cb40258cdb86b0285f38fe26504.jpg?fit=crop&h=24&w=24',
    name = 'Postre lácteo natural +Proteínas Hacendado 0% MG 10 g proteínas', package_size = 'Bote 0.5 kg', price_eur = 1.40,
    url = 'https://tienda.mercadona.es/product/21256'
  WHERE id = 'mercadona-yogurt' AND external_id IS NULL AND name = 'Yogur proteína';

UPDATE store_product SET
    external_id = '3724', image_url = 'https://prod-mercadona.imgix.net/images/626ed2bc08b4d92dc0d786b7c368a8c9.jpeg?fit=crop&h=24&w=24',
    name = 'Pechugas enteras de pollo', package_size = 'Bandeja 0.5 kg', price_eur = 3.33,
    url = 'https://tienda.mercadona.es/product/3724'
  WHERE id = 'mercadona-chicken' AND external_id IS NULL AND name = 'Pechugas enteras de pollo';

UPDATE store_product SET
    external_id = '5710', image_url = 'https://prod-mercadona.imgix.net/images/42caa18db79c87be197d47cdb41c0e28.jpg?fit=crop&h=24&w=24',
    name = 'Pechuga 92% pavo Hacendado lonchas', package_size = 'Paquete 0.2 kg', price_eur = 2.85,
    url = 'https://tienda.mercadona.es/product/5710'
  WHERE id = 'mercadona-turkey' AND external_id IS NULL AND name = 'Pavo lonchas/corte';

UPDATE store_product SET
    external_id = '18018', image_url = 'https://prod-mercadona.imgix.net/images/69aa188c64eb23f4855d6739832b09c1.jpg?fit=crop&h=24&w=24',
    name = 'Atún claro al natural Hacendado', package_size = '0.48 kg', price_eur = 4.20,
    url = 'https://tienda.mercadona.es/product/18018'
  WHERE id = 'mercadona-tuna' AND external_id IS NULL AND name = 'Atún natural';

UPDATE store_product SET
    external_id = '82610.1', image_url = 'https://prod-mercadona.imgix.net/images/f9915fd0e276f93caa4eb284fbbcbc34.jpg?fit=crop&h=24&w=24',
    name = 'Merluza a rodajas', package_size = 'Pieza 1.12 kg', price_eur = 12.88,
    url = 'https://tienda.mercadona.es/product/82610.1'
  WHERE id = 'mercadona-fish' AND external_id IS NULL AND name = 'Merluza';

UPDATE store_product SET
    external_id = '87204', image_url = 'https://prod-mercadona.imgix.net/images/b072fbeed617fa30e152c33cd8eb8fbb.jpg?fit=crop&h=24&w=24',
    name = 'Filete de salmón', package_size = 'Bandeja 0.35 kg', price_eur = 8.05,
    url = 'https://tienda.mercadona.es/product/87204'
  WHERE id = 'mercadona-salmon' AND external_id IS NULL AND name = 'Salmón';

UPDATE store_product SET
    external_id = '5044', image_url = 'https://prod-mercadona.imgix.net/images/0daf43fb5761b823ce83c985930c97c9.jpg?fit=crop&h=24&w=24',
    name = 'Arroz redondo Hacendado', package_size = 'Paquete 1 kg', price_eur = 1.20,
    url = 'https://tienda.mercadona.es/product/5044'
  WHERE id = 'mercadona-rice' AND external_id IS NULL AND name = 'Arroz redondo/largo';

UPDATE store_product SET
    external_id = '35778', image_url = 'https://prod-mercadona.imgix.net/images/3e10d3e036f04703c87e31a01d680ebc.jpg?fit=crop&h=24&w=24',
    name = 'Spaghetti integral Hacendado', package_size = 'Paquete 0.5 kg', price_eur = 1.15,
    url = 'https://tienda.mercadona.es/product/35778'
  WHERE id = 'mercadona-whole-wheat-pasta' AND external_id IS NULL AND name = 'Pasta integral';

UPDATE store_product SET
    external_id = '69099', image_url = 'https://prod-mercadona.imgix.net/images/a66b8d4177a91f7f219903267291e071.jpg?fit=crop&h=24&w=24',
    name = 'Patatas', package_size = 'Malla 5 kg', price_eur = 4.75,
    url = 'https://tienda.mercadona.es/product/69099'
  WHERE id = 'mercadona-potato' AND external_id IS NULL AND name = 'Patatas';

UPDATE store_product SET
    external_id = '12049.1', image_url = 'https://prod-mercadona.imgix.net/images/af7f030f73aeb80be5996256f1b3f44f.jpg?fit=crop&h=24&w=24',
    name = 'Pan integral trigo 100%', package_size = '0.35 kg', price_eur = 1.50,
    url = 'https://tienda.mercadona.es/product/12049.1'
  WHERE id = 'mercadona-whole-wheat-bread' AND external_id IS NULL AND name = 'Pan integral';

UPDATE store_product SET
    external_id = '52534', image_url = 'https://prod-mercadona.imgix.net/images/cda982b20663598e095ee29141b906fc.jpg?fit=crop&h=24&w=24',
    name = 'Menestra de verduras Hacendado ultracongelada', package_size = 'Paquete 1 kg', price_eur = 1.75,
    url = 'https://tienda.mercadona.es/product/52534'
  WHERE id = 'mercadona-vegetables' AND external_id IS NULL AND name = 'Verdura variada';

UPDATE store_product SET
    external_id = '69706', image_url = 'https://prod-mercadona.imgix.net/images/521b7ea477692d5b5ae5835f2aea842c.jpg?fit=crop&h=24&w=24',
    name = 'Ensalada mezcla 4 estaciones lavada', package_size = 'Paquete 0.25 kg', price_eur = 0.85,
    url = 'https://tienda.mercadona.es/product/69706'
  WHERE id = 'mercadona-salad' AND external_id IS NULL AND name = 'Ensaladas preparadas';

UPDATE store_product SET
    external_id = '4740', image_url = 'https://prod-mercadona.imgix.net/images/a5648e373920a10023a7ab6304eb0dc0.jpg?fit=crop&h=24&w=24',
    name = 'Aceite de oliva virgen extra Hacendado', package_size = 'Botella 1 l', price_eur = 4.80,
    url = 'https://tienda.mercadona.es/product/4740'
  WHERE id = 'mercadona-olive-oil' AND external_id IS NULL AND name = 'Aceite de oliva virgen extra';

UPDATE store_product SET
    external_id = '86809', image_url = 'https://prod-mercadona.imgix.net/images/41dadffa039844fae74ffe0981c13c00.jpg?fit=crop&h=24&w=24',
    name = 'Cocktail frutos secos natural Hacendado', package_size = 'Paquete 0.2 kg', price_eur = 2.60,
    url = 'https://tienda.mercadona.es/product/86809'
  WHERE id = 'mercadona-almonds-walnuts' AND external_id IS NULL AND name = 'Frutos secos naturales';

UPDATE store_product SET
    external_id = '61089', image_url = 'https://prod-mercadona.imgix.net/images/9226d0c69488fd575c374293d5538ad6.jpg?fit=crop&h=24&w=24',
    name = 'Mix frutos rojos Hacendado ultracongeladas', package_size = 'Paquete 0.3 kg', price_eur = 1.90,
    url = 'https://tienda.mercadona.es/product/61089'
  WHERE id = 'mercadona-berries' AND external_id IS NULL AND name = 'Frutos rojos congelados';

UPDATE store_product SET
    external_id = '10384', image_url = 'https://prod-mercadona.imgix.net/images/c1efb0bda5d2691380b626b729131599.jpg?fit=crop&h=24&w=24',
    name = 'Leche desnatada Hacendado', package_size = 'Brik 1 l', price_eur = 0.82,
    url = 'https://tienda.mercadona.es/product/10384'
  WHERE id = 'mercadona-skim-milk' AND external_id IS NULL AND name = 'Leche desnatada';
