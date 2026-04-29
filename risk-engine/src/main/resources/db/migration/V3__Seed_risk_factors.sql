INSERT INTO risk_rules.risk_factors (factor_key, factor_label, category, base_score_impact, premium_multiplier, description)
VALUES
    ('driver_age_under_25',  'Driver under 25 years old',              'driver',   15.00, 1.250, 'Young drivers have statistically higher accident rates'),
    ('driver_age_over_70',   'Driver over 70 years old',               'driver',   10.00, 1.150, 'Senior drivers have increased risk'),
    ('license_under_2yr',    'License held less than 2 years',         'driver',   12.00, 1.200, 'Limited driving experience'),
    ('violation_count_1',    '1 traffic violation in last 5 years',    'driver',    8.00, 1.100, 'Minor driving record concern'),
    ('violation_count_2plus','2 or more violations in last 5 years',   'driver',   18.00, 1.300, 'Significant driving record concern'),
    ('accident_count_1',     '1 at-fault accident in last 5 years',    'driver',   10.00, 1.150, 'Previous accident history'),
    ('accident_count_2plus', '2 or more at-fault accidents',           'driver',   22.00, 1.450, 'High accident history'),
    ('vehicle_sports',       'Sports or performance vehicle',          'vehicle',  12.00, 1.200, 'Higher repair costs and liability'),
    ('vehicle_luxury',       'Luxury vehicle',                         'vehicle',   8.00, 1.150, 'High repair and replacement costs'),
    ('vehicle_age_over_15',  'Vehicle older than 15 years',            'vehicle',   6.00, 1.100, 'Older vehicles have higher mechanical risk'),
    ('coverage_comprehensive','Comprehensive coverage selected',       'coverage',  0.00, 1.300, 'Covers theft, weather, non-collision damage'),
    ('coverage_collision',   'Collision coverage selected',            'coverage',  0.00, 1.150, 'Covers collision damage to own vehicle'),
    ('coverage_full',        'Full coverage selected',                 'coverage',  0.00, 1.500, 'All perils coverage'),
    ('high_density_zip',     'High-density urban area',                'location',  5.00, 1.080, 'Urban areas have higher accident frequency')
ON CONFLICT (factor_key) DO NOTHING;
