-- Gives back the shop reference that editing a product used to take away.
--
-- Additive on top of V41 (ADR-003). Until this migration's companion fix, StoreProductService.update
-- rebuilt the row through a constructor that defaulted external_id and image_url to null, so saving
-- the edit form on an imported product dropped both: the thumbnail disappeared from the list and the
-- refresh action stopped being offered, because the screen only offers it where a source exists. The
-- rows are still there and still correct in everything the admin typed -- what they lost is where
-- they came from, and only V40 knows that.
--
-- SO THIS RE-STATES V40'S MATCHES, AND NOTHING ELSE. Name, package, price and link are left exactly
-- as they stand: those are the fields an admin edits on purpose, and the whole reason a row is here
-- is that somebody edited it. Only the two columns the bug erased are written, and image_url only
-- where it is still empty (COALESCE), so a photo somebody chose by hand outlives this.
--
-- WHY THE GUARDS: `external_id IS NULL` skips every row that kept its reference -- nothing to repair
-- -- and every row refreshed or re-imported since. `store = 'MERCADONA'` skips a product moved to
-- another shelf: V41 gave OTRAS to what no chain sells, and a Mercadona id would be a lie there. The
-- NOT EXISTS keeps UNIQUE (store, external_id) honest -- if the same product was re-imported as a
-- new row while this one sat broken, the id belongs to that row and this one is left as it is.
--
-- ONE STATEMENT PER ROW, like V40: `UPDATE ... FROM (VALUES ...)` would say it once, but the test
-- profile runs these migrations on H2 in PostgreSQL mode (application-test.yml) and H2 does not
-- accept that form. Repetition that runs on both engines beats concision that only runs on one.
--
-- The two rows V40 left alone on purpose ('mercadona-whey-protein', 'mercadona-sweet-potato') are
-- absent here too: the shop does not sell them, so there is still no source to point at.

UPDATE store_product SET
    external_id = '86341', image_url = COALESCE(image_url, 'https://prod-mercadona.imgix.net/images/98910a7833008eddea7deffc843ffac7.jpg?fit=crop&h=24&w=24')
  WHERE id = 'mercadona-oats' AND external_id IS NULL AND store = 'MERCADONA'
    AND NOT EXISTS (
      SELECT 1 FROM store_product other
        WHERE other.store = 'MERCADONA' AND other.external_id = '86341');

UPDATE store_product SET
    external_id = '3819', image_url = COALESCE(image_url, 'https://prod-mercadona.imgix.net/images/e4a37940916985bf5ca166e266580c37.jpg?fit=crop&h=24&w=24')
  WHERE id = 'mercadona-banana' AND external_id IS NULL AND store = 'MERCADONA'
    AND NOT EXISTS (
      SELECT 1 FROM store_product other
        WHERE other.store = 'MERCADONA' AND other.external_id = '3819');

UPDATE store_product SET
    external_id = '31504', image_url = COALESCE(image_url, 'https://prod-mercadona.imgix.net/images/707625e3e76b36f1834d56c1ed8aa8df.jpg?fit=crop&h=24&w=24')
  WHERE id = 'mercadona-eggs' AND external_id IS NULL AND store = 'MERCADONA'
    AND NOT EXISTS (
      SELECT 1 FROM store_product other
        WHERE other.store = 'MERCADONA' AND other.external_id = '31504');

UPDATE store_product SET
    external_id = '31312', image_url = COALESCE(image_url, 'https://prod-mercadona.imgix.net/images/cde7e603fa71c589abe8e10279bd36ab.jpg?fit=crop&h=24&w=24')
  WHERE id = 'mercadona-egg-whites' AND external_id IS NULL AND store = 'MERCADONA'
    AND NOT EXISTS (
      SELECT 1 FROM store_product other
        WHERE other.store = 'MERCADONA' AND other.external_id = '31312');

UPDATE store_product SET
    external_id = '51071', image_url = COALESCE(image_url, 'https://prod-mercadona.imgix.net/images/d2f12f4f6b2de080b5aeebc8aa1a5e46.jpg?fit=crop&h=24&w=24')
  WHERE id = 'mercadona-fresh-cheese' AND external_id IS NULL AND store = 'MERCADONA'
    AND NOT EXISTS (
      SELECT 1 FROM store_product other
        WHERE other.store = 'MERCADONA' AND other.external_id = '51071');

UPDATE store_product SET
    external_id = '21256', image_url = COALESCE(image_url, 'https://prod-mercadona.imgix.net/images/7ad06cb40258cdb86b0285f38fe26504.jpg?fit=crop&h=24&w=24')
  WHERE id = 'mercadona-yogurt' AND external_id IS NULL AND store = 'MERCADONA'
    AND NOT EXISTS (
      SELECT 1 FROM store_product other
        WHERE other.store = 'MERCADONA' AND other.external_id = '21256');

UPDATE store_product SET
    external_id = '3724', image_url = COALESCE(image_url, 'https://prod-mercadona.imgix.net/images/626ed2bc08b4d92dc0d786b7c368a8c9.jpeg?fit=crop&h=24&w=24')
  WHERE id = 'mercadona-chicken' AND external_id IS NULL AND store = 'MERCADONA'
    AND NOT EXISTS (
      SELECT 1 FROM store_product other
        WHERE other.store = 'MERCADONA' AND other.external_id = '3724');

UPDATE store_product SET
    external_id = '5710', image_url = COALESCE(image_url, 'https://prod-mercadona.imgix.net/images/42caa18db79c87be197d47cdb41c0e28.jpg?fit=crop&h=24&w=24')
  WHERE id = 'mercadona-turkey' AND external_id IS NULL AND store = 'MERCADONA'
    AND NOT EXISTS (
      SELECT 1 FROM store_product other
        WHERE other.store = 'MERCADONA' AND other.external_id = '5710');

UPDATE store_product SET
    external_id = '18018', image_url = COALESCE(image_url, 'https://prod-mercadona.imgix.net/images/69aa188c64eb23f4855d6739832b09c1.jpg?fit=crop&h=24&w=24')
  WHERE id = 'mercadona-tuna' AND external_id IS NULL AND store = 'MERCADONA'
    AND NOT EXISTS (
      SELECT 1 FROM store_product other
        WHERE other.store = 'MERCADONA' AND other.external_id = '18018');

UPDATE store_product SET
    external_id = '82610.1', image_url = COALESCE(image_url, 'https://prod-mercadona.imgix.net/images/f9915fd0e276f93caa4eb284fbbcbc34.jpg?fit=crop&h=24&w=24')
  WHERE id = 'mercadona-fish' AND external_id IS NULL AND store = 'MERCADONA'
    AND NOT EXISTS (
      SELECT 1 FROM store_product other
        WHERE other.store = 'MERCADONA' AND other.external_id = '82610.1');

UPDATE store_product SET
    external_id = '87204', image_url = COALESCE(image_url, 'https://prod-mercadona.imgix.net/images/b072fbeed617fa30e152c33cd8eb8fbb.jpg?fit=crop&h=24&w=24')
  WHERE id = 'mercadona-salmon' AND external_id IS NULL AND store = 'MERCADONA'
    AND NOT EXISTS (
      SELECT 1 FROM store_product other
        WHERE other.store = 'MERCADONA' AND other.external_id = '87204');

UPDATE store_product SET
    external_id = '5044', image_url = COALESCE(image_url, 'https://prod-mercadona.imgix.net/images/0daf43fb5761b823ce83c985930c97c9.jpg?fit=crop&h=24&w=24')
  WHERE id = 'mercadona-rice' AND external_id IS NULL AND store = 'MERCADONA'
    AND NOT EXISTS (
      SELECT 1 FROM store_product other
        WHERE other.store = 'MERCADONA' AND other.external_id = '5044');

UPDATE store_product SET
    external_id = '35778', image_url = COALESCE(image_url, 'https://prod-mercadona.imgix.net/images/3e10d3e036f04703c87e31a01d680ebc.jpg?fit=crop&h=24&w=24')
  WHERE id = 'mercadona-whole-wheat-pasta' AND external_id IS NULL AND store = 'MERCADONA'
    AND NOT EXISTS (
      SELECT 1 FROM store_product other
        WHERE other.store = 'MERCADONA' AND other.external_id = '35778');

UPDATE store_product SET
    external_id = '69099', image_url = COALESCE(image_url, 'https://prod-mercadona.imgix.net/images/a66b8d4177a91f7f219903267291e071.jpg?fit=crop&h=24&w=24')
  WHERE id = 'mercadona-potato' AND external_id IS NULL AND store = 'MERCADONA'
    AND NOT EXISTS (
      SELECT 1 FROM store_product other
        WHERE other.store = 'MERCADONA' AND other.external_id = '69099');

UPDATE store_product SET
    external_id = '12049.1', image_url = COALESCE(image_url, 'https://prod-mercadona.imgix.net/images/af7f030f73aeb80be5996256f1b3f44f.jpg?fit=crop&h=24&w=24')
  WHERE id = 'mercadona-whole-wheat-bread' AND external_id IS NULL AND store = 'MERCADONA'
    AND NOT EXISTS (
      SELECT 1 FROM store_product other
        WHERE other.store = 'MERCADONA' AND other.external_id = '12049.1');

UPDATE store_product SET
    external_id = '52534', image_url = COALESCE(image_url, 'https://prod-mercadona.imgix.net/images/cda982b20663598e095ee29141b906fc.jpg?fit=crop&h=24&w=24')
  WHERE id = 'mercadona-vegetables' AND external_id IS NULL AND store = 'MERCADONA'
    AND NOT EXISTS (
      SELECT 1 FROM store_product other
        WHERE other.store = 'MERCADONA' AND other.external_id = '52534');

UPDATE store_product SET
    external_id = '69706', image_url = COALESCE(image_url, 'https://prod-mercadona.imgix.net/images/521b7ea477692d5b5ae5835f2aea842c.jpg?fit=crop&h=24&w=24')
  WHERE id = 'mercadona-salad' AND external_id IS NULL AND store = 'MERCADONA'
    AND NOT EXISTS (
      SELECT 1 FROM store_product other
        WHERE other.store = 'MERCADONA' AND other.external_id = '69706');

UPDATE store_product SET
    external_id = '4740', image_url = COALESCE(image_url, 'https://prod-mercadona.imgix.net/images/a5648e373920a10023a7ab6304eb0dc0.jpg?fit=crop&h=24&w=24')
  WHERE id = 'mercadona-olive-oil' AND external_id IS NULL AND store = 'MERCADONA'
    AND NOT EXISTS (
      SELECT 1 FROM store_product other
        WHERE other.store = 'MERCADONA' AND other.external_id = '4740');

UPDATE store_product SET
    external_id = '86809', image_url = COALESCE(image_url, 'https://prod-mercadona.imgix.net/images/41dadffa039844fae74ffe0981c13c00.jpg?fit=crop&h=24&w=24')
  WHERE id = 'mercadona-almonds-walnuts' AND external_id IS NULL AND store = 'MERCADONA'
    AND NOT EXISTS (
      SELECT 1 FROM store_product other
        WHERE other.store = 'MERCADONA' AND other.external_id = '86809');

UPDATE store_product SET
    external_id = '61089', image_url = COALESCE(image_url, 'https://prod-mercadona.imgix.net/images/9226d0c69488fd575c374293d5538ad6.jpg?fit=crop&h=24&w=24')
  WHERE id = 'mercadona-berries' AND external_id IS NULL AND store = 'MERCADONA'
    AND NOT EXISTS (
      SELECT 1 FROM store_product other
        WHERE other.store = 'MERCADONA' AND other.external_id = '61089');

UPDATE store_product SET
    external_id = '10384', image_url = COALESCE(image_url, 'https://prod-mercadona.imgix.net/images/c1efb0bda5d2691380b626b729131599.jpg?fit=crop&h=24&w=24')
  WHERE id = 'mercadona-skim-milk' AND external_id IS NULL AND store = 'MERCADONA'
    AND NOT EXISTS (
      SELECT 1 FROM store_product other
        WHERE other.store = 'MERCADONA' AND other.external_id = '10384');
