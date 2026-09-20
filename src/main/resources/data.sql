TRUNCATE TABLE stock, clientes, productos RESTART IDENTITY CASCADE;

INSERT INTO productos (nombre, descripcion, precio) VALUES
    ('Laptop Gamer',          'Intel i7, 16GB RAM, RTX 4060',    1299.99),
    ('Monitor 27" 4K',        'Panel IPS, 144Hz, HDR600',         499.00),
    ('Teclado Mecanico RGB',  'Switches Red, layout ES',           89.90),
    ('Mouse Inalambrico',     'Sensor 16000 DPI, 2.4GHz',          45.50);

INSERT INTO clientes (nombre, email, telefono, direccion) VALUES
    ('Juan Perez',    'juan@example.com',   '+34 600 111 222', 'Calle Mayor 1, Madrid'),
    ('Maria Garcia',  'maria@example.com',  '+34 600 333 444', 'Avda. Principal 5, Barcelona'),
    ('Carlos Lopez',  'carlos@example.com', '+34 600 555 666', 'Calle Sol 9, Valencia');

INSERT INTO stock (producto_id, cantidad) VALUES
    (1, 12),
    (2, 30),
    (3, 75),
    (4, 40);