(ns cabinetcoord.store
  "SSoT for the ISCO-08 7522 cabinet-makers workshop scheduling/logistics
  coordination actor (itonami actor pattern, ADR-2607121000 / CLAUDE.md
  Actors section; README's 'Robotics premise' — a workshop
  scheduling/logistics coordination robot performs crew scheduling,
  job/commission/progress-record logging and timber/hardware-materials
  supply-order coordination for a cabinet-making crew under this
  advisor/governor pair, which never dispatches hardware itself, never
  performs woodworking work itself, and never finalizes a
  woodworking-execution decision or a workshop-safety-clearance
  decision, and never overrides a shop safety officer's judgment —
  those remain the shop safety officer's exclusive judgment). Modeled
  closely on cloud-itonami-isco-7521's woodtreatcoord.store.

  Domain:

    maker    — a registered cabinet-making crew member (:maker-id,
              :name)
    workshop — a registered cabinet-making workshop site {:workshop-id
              :name :max-supply-cost number}. `:max-supply-cost` is an
              informational registered ceiling used only to decide
              whether a `:coordinate-supply-order` proposal escalates
              to human sign-off (the governor never blocks a
              within-threshold order outright; it only decides
              commit vs. escalate).
    record   — a committed operating record (a logged job/commission/
              progress entry, a scheduled crew/task operation, a
              flagged safety concern, or a coordinated timber/hardware-
              materials supply order) — written ONLY via commit-record!.
    ledger   — append-only audit trail, commit or hold.")

(defprotocol Store
  (maker [s maker-id])
  (workshop [s workshop-id])
  (records-of [s maker-id])
  (ledger [s])
  (register-maker! [s maker])
  (register-workshop! [s workshop])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (maker [_ maker-id] (get-in @a [:makers maker-id]))
  (workshop [_ workshop-id] (get-in @a [:workshops workshop-id]))
  (records-of [_ maker-id] (filter #(= maker-id (:maker-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-maker! [s m]
    (swap! a assoc-in [:makers (:maker-id m)] m) s)
  (register-workshop! [s w]
    (swap! a assoc-in [:workshops (:workshop-id w)] w) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:makers {} :workshops {} :records [] :ledger []}
                                    seed)))))
