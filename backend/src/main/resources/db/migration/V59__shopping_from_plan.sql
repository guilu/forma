-- =================================================================================================
-- V59 — Lo que hace falta para que la lista de la compra salga del plan.
--
-- Dos huecos del modelo, ninguno de ellos una decisión nueva: los dos son campos que ya existían
-- vacíos o que mentían por no admitir un nulo.
-- =================================================================================================

-- -------------------------------------------------------------------------------------------------
-- 1. Cuánto trae cada envase.
--
-- V48 añadió `package_amount` y `package_unit` con un comentario que decía que la pareja honesta es
-- la cantidad y la unidad que declara la tienda. Nunca las rellenó nadie, así que lo único legible
-- seguía siendo `package_size`, texto libre y desigual: «500 g», «1 kg», «12 uds», «kg» a secas.
--
-- Aquí se pueblan SOLO los que se leen sin ambigüedad. Se guarda lo que dice la tienda —1 KG, no
-- 1000 G— porque convertir es aritmética y puede hacerse al calcular; inventar no.
--
-- Los otros catorce se quedan a NULL a propósito. «kg» en los plátanos significa que se venden a
-- granel y «bolsa» que la tienda no dice cuánto pesa: poner un número ahí sería decidir por la
-- tienda. Sin cantidad, la lista pedirá una unidad, que es lo que alguien apuntaría a mano.
-- -------------------------------------------------------------------------------------------------
UPDATE store_product SET package_amount = 500, package_unit = 'G'  WHERE id = 'mercadona-oats';
UPDATE store_product SET package_amount = 1,   package_unit = 'KG' WHERE id = 'mercadona-whey-protein';
UPDATE store_product SET package_amount = 12,  package_unit = 'UD' WHERE id = 'mercadona-eggs';
UPDATE store_product SET package_amount = 1,   package_unit = 'KG' WHERE id = 'mercadona-rice';
UPDATE store_product SET package_amount = 500, package_unit = 'G'  WHERE id = 'mercadona-whole-wheat-pasta';
UPDATE store_product SET package_amount = 5,   package_unit = 'KG' WHERE id = 'mercadona-potato';
UPDATE store_product SET package_amount = 1,   package_unit = 'L'  WHERE id = 'mercadona-olive-oil';
UPDATE store_product SET package_amount = 200, package_unit = 'G'  WHERE id = 'mercadona-almonds-walnuts';
UPDATE store_product SET package_amount = 1,   package_unit = 'L'  WHERE id = 'mercadona-skim-milk';

-- -------------------------------------------------------------------------------------------------
-- 2. Un precio que no se sabe.
--
-- `estimated_cost_eur` era NOT NULL, así que un alimento del plan sin producto en el catálogo solo
-- podía entrar a 0,00 €. Y cero no es «no lo sé»: cero dice que es gratis, y el presupuesto de la
-- semana se lo creía.
--
-- Ahora admite nulo. Lo que falta por catalogar se ve en la lista —que es justo para lo que hay que
-- verlo— sin arrastrar el total hacia abajo con un precio que nadie ha dicho.
-- -------------------------------------------------------------------------------------------------
ALTER TABLE shopping_list_items ALTER COLUMN estimated_cost_eur DROP NOT NULL;
