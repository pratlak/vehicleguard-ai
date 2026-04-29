INSERT INTO risk_rules.vehicle_base_rates (vehicle_category, base_annual_premium, description)
VALUES
    ('sedan',   1100.00, 'Standard sedan - Toyota Camry, Honda Civic, etc.'),
    ('suv',     1300.00, 'SUV - Ford Explorer, Toyota RAV4, etc.'),
    ('truck',   1400.00, 'Pickup truck - Ford F-150, Chevy Silverado, etc.'),
    ('sports',  1800.00, 'Sports car - Ford Mustang, Chevrolet Camaro, etc.'),
    ('luxury',  2200.00, 'Luxury vehicle - BMW 7-Series, Mercedes S-Class, etc.'),
    ('electric',1600.00, 'Electric vehicle - Tesla, Rivian, etc.')
ON CONFLICT (vehicle_category) DO NOTHING;
