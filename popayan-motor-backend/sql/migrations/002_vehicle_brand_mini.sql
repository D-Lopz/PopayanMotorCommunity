-- v1.3: catálogo de marcas. El vehículo id 2 (placa NDS880) se registró con la marca "Mini Cooper"
-- y el modelo "CountryMan"; la marca es Mini y el modelo Cooper Countryman.
-- Solo cambia esa fila y solo si sigue con los valores originales: si alguien ya la editó,
-- no hace nada (0 rows affected). Se puede ejecutar más de una vez sin efecto extra:
--   mysql -u root -p popayan_motor < sql/migrations/002_vehicle_brand_mini.sql
USE popayan_motor;

UPDATE vehicle
SET brand = 'Mini', model = 'Cooper Countryman'
WHERE id = 2 AND plate = 'NDS880' AND brand = 'Mini Cooper' AND model = 'CountryMan';
