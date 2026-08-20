-- Referential integrity: investment_score was keyed only by
-- investment_canonical_key (TEXT), with no FK to investment.id, so a
-- deleted investment could leave an orphaned score row behind (see docs
-- review - "investment_score lacks a formal FK to investment" finding).
--
-- investment_id is nullable because scoring for a *newly discovered*
-- investment happens before that investment is upserted into the
-- `investment` table within the same scan (see MonitoringService.
-- processIfNew, called before commitInvestments) - there is no id to
-- reference yet at that point. JdbcInvestmentScoreRepository.save now
-- resolves investment_id via a canonical_key subquery on every save, so
-- it self-heals on the next scan/rescore once the investment row exists.
-- ON DELETE CASCADE ensures the score row is cleaned up automatically if
-- the investment is ever deleted.
ALTER TABLE investment_score ADD COLUMN investment_id INTEGER REFERENCES investment(id) ON DELETE CASCADE;

UPDATE investment_score
SET investment_id = (
    SELECT id FROM investment WHERE investment.canonical_key = investment_score.investment_canonical_key
)
WHERE investment_id IS NULL;

CREATE INDEX idx_investment_score_investment_id ON investment_score(investment_id);
