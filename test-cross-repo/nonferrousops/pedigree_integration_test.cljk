(ns nonferrousops.pedigree-integration-test
  "ADR-2607999980's end-to-end proof for THIS repo's half of the
  smartphone-chain cross-actor supply-chain-linkage chain (DIRECT
  PORT of ADR-2607999970's `ironops.pedigree-integration-test`). THIS
  REPO IS THE ORIGIN of the smartphone-chain pedigree chain (non-
  ferrous metal ore mining, ISIC 0729) -- unlike a downstream sibling
  repo's own `:cross-repo-test` alias (which calls INTO an upstream
  sibling repo's real export/robotics functions), this repo has no
  upstream sibling of its own to cross into, so there is no foreign
  `:local/root` dependency to declare here (see deps.edn's
  `:cross-repo-test` alias comment for the full reasoning).

  What this test proves instead: a GENUINE round-trip through this
  actor's OWN real `nonferrousops.store` -- a production record is
  actually written via `nonferrousops.store/add-production-record`,
  actually read back via `nonferrousops.store/production-record`, and
  ONLY THEN handed to `nonferrousops.export/pedigree-for-production-
  record` -- never a hand-typed map passed straight to the export fn
  without ever having been 'on file'. This is the SAME 'real data,
  not a fixture' discipline every sibling `pedigree-integration-test`
  in this fleet applies to a REAL upstream simulation; here applied
  to this actor's REAL store, since it has no upstream simulation (or
  upstream actor at all) to call into.

  Downstream (`cloud-itonami-isic-2610`, ADR-2607999980 Part 2)
  proves the OTHER half of this link: a real cross-repo call INTO
  THIS repo's `nonferrousops.export/pedigree-for-production-record`,
  via its own `:cross-repo-test` alias declaring THIS repo as a
  `:local/root` sibling.

  Run with `clojure -M:dev:cross-repo-test`. Still no live network
  call at runtime: this is a build-time classpath dependency exercised
  by tests, same category as every other alias in this fleet."
  (:require [clojure.test :refer [deftest is testing]]
            [kotoba.pedigree :as pedigree]
            [nonferrousops.export :as export]
            [nonferrousops.store :as store]))

(deftest real-store-round-trip-pedigree-is-shape-valid
  (testing "a pedigree built from a production record that ACTUALLY went through nonferrousops.store's real write+read path passes kotoba.pedigree/valid?"
    (let [st (store/mem-store)
          st' (store/add-production-record st "prod-real-1"
                                            {:site-id "copper-site-001"
                                             :commodity :copper
                                             :claimed-grade 26.5
                                             :grade-actual 26.5
                                             :grade-min 20.0
                                             :grade-max 30.0
                                             :quantity-tonnes 3600.0
                                             :price-per-unit 8500.0})
          rec (store/production-record st' "prod-real-1")
          p (export/pedigree-for-production-record rec "2026-07-16")]
      (is (some? p))
      (is (true? (pedigree/valid? p)))
      (is (= "cloud-itonami-isic-0729" (:pedigree/issuing-actor p)))
      (testing "the claim is the record's REAL stored reading, not a value invented at test-call-time -- independently re-reading the same store yields the identical number"
        (is (= (:grade-actual (store/production-record st' "prod-real-1"))
               (pedigree/claim-value p :grade-actual)))
        (is (= 26.5 (pedigree/claim-value p :grade-actual))
            "documents the actual real-record value, for a human reader's sanity")))))
