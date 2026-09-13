INSERT INTO process_stage_definition VALUES
('00000000-0000-0000-0000-000000000701', 'MASH_PH_CHECK', 'MASHING', 85, 'Verificar pH de maceración', 'Medir pH, registrar temperatura de la muestra y aplicar corrección autorizada.', FALSE, 'ALL', TRUE),
('00000000-0000-0000-0000-000000000702', 'PRE_BOIL_CHECK', 'MASHING', 145, 'Control pre-cocción', 'Registrar volumen, densidad y condición del mosto antes del hervor.', FALSE, 'ALL', TRUE),
('00000000-0000-0000-0000-000000000703', 'POST_BOIL_CHECK', 'BOILING', 175, 'Control post-cocción', 'Registrar volumen, densidad y rendimiento al finalizar cocción y whirlpool.', FALSE, 'ALL', TRUE),
('00000000-0000-0000-0000-000000000704', 'DRY_HOPPING', 'CELLAR', 225, 'Dry hopping', 'Registrar lúpulo, lote, cantidad, momento, temperatura y tiempo de contacto.', TRUE, 'ALL', TRUE),
('00000000-0000-0000-0000-000000000705', 'YEAST_TRUB_MANAGEMENT', 'CELLAR', 235, 'Purgar sólidos o cosechar levadura', 'Registrar purgas de trub y cosecha, descarte o reutilización de levadura.', TRUE, 'ALL', TRUE),
('00000000-0000-0000-0000-000000000706', 'BRIGHT_TANK_TRANSFER', 'CELLAR', 255, 'Transferir a tanque brillante', 'Transferir cerveza acondicionada cuando el proceso utilice tanque brillante.', TRUE, 'ALL', TRUE),
('00000000-0000-0000-0000-000000000707', 'PASTEURIZATION', 'PACKAGING', 325, 'Pasteurización', 'Aplicar y registrar unidades de pasteurización cuando el producto lo requiera.', TRUE, 'ALL', TRUE),
('00000000-0000-0000-0000-000000000708', 'PACKAGE_QUALITY_CHECK', 'PACKAGING', 335, 'Control de calidad de empaque', 'Verificar llenado, cierre, fugas, oxígeno, carbonatación, codificado y presentación.', FALSE, 'ALL', TRUE);
