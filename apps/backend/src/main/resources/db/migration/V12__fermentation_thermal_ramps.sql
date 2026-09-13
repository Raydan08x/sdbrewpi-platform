ALTER TABLE fermentation_profile_step ADD COLUMN ramp_rate_c_per_hour NUMERIC(5,2);

ALTER TABLE fermentation_profile_step ADD CONSTRAINT chk_profile_step_ramp_rate
  CHECK (ramp_rate_c_per_hour IS NULL OR (ramp_rate_c_per_hour >= 0.10 AND ramp_rate_c_per_hour <= 10.00));

UPDATE fermentation_profile_step
SET ramp_rate_c_per_hour = CASE step_order
  WHEN 2 THEN 0.50
  WHEN 3 THEN 1.00
  ELSE NULL
END
WHERE recipe_version_id = '00000000-0000-0000-0000-000000000101';
