# Design System Strategy: The Precision Ledger

## 1. Overview & Creative North Star
**Creative North Star: "The Digital Curator"**
The objective of this design system is to transcend the generic "SaaS dashboard" aesthetic. For a Smart POS + Inventory system, the UI must feel as precise as a Swiss watch but as fluid as a modern editorial magazine. We move away from "boxed-in" layouts toward an **expansive, tonal hierarchy**.

By utilizing "The Digital Curator" approach, we treat every data point—from stock levels to Rupiah transactions—as a curated piece of information. We break the rigid grid through **intentional white space** and **asymmetric density**, where the primary Emerald (#10b981) serves as a surgical strike of color against a sophisticated, layered Slate backdrop.

---

## 2. Colors & Surface Philosophy
This system rejects the "standard" 1px border. We define space through **chromatic depth** rather than structural lines.

### The Palette (Material Design Mapping)
*   **Primary (Emerald):** `#006c49` (Action) | `#10b981` (Container)
*   **Surface Foundation:** `#f8f9ff` (Background)
*   **Tonal Accents:** Slate/Gray transitions from Surface Low (`#eff4ff`) to Highest (`#d3e4fe`).

### The "No-Line" Rule
**Explicit Instruction:** Do not use 1px solid borders to section off the dashboard. Boundaries must be defined by background color shifts. 
*   *Example:* A "Quick Sale" panel should not be a bordered box; it should be a `surface-container-low` area sitting atop the `surface` background.

### Surface Hierarchy & Nesting
Treat the UI as a series of stacked sheets of fine paper.
*   **Level 0 (Base):** `surface` (#f8f9ff) — The canvas.
*   **Level 1 (Sections):** `surface-container-low` (#eff4ff) — Large layout blocks.
*   **Level 2 (Cards):** `surface-container-lowest` (#ffffff) — High-density data cards.

### The "Glass & Gradient" Rule
To elevate the "Smart" aspect of the POS, floating elements (like Modals or Toast notifications) must use **Glassmorphism**:
*   **Apply:** `surface-container-lowest` at 80% opacity + 12px Backdrop Blur.
*   **CTAs:** Use a subtle linear gradient from `primary` (#006c49) to `primary-container` (#10b981) at a 135° angle to provide "soul" and tactile depth to buttons.

---

## 3. Typography: The Editorial Scale
We use **Inter** not just for legibility, but as a brand anchor. The contrast between high-density labels and sweeping display titles creates an authoritative feel.

*   **Display (Rp Totals):** `display-md` (2.75rem). Use for daily revenue. Tracked at -0.02em for a premium, "tight" look.
*   **Headlines (Section Titles):** `headline-sm` (1.5rem). Semi-bold.
*   **Data Points:** `body-md` (0.875rem) for inventory lists.
*   **Metadata:** `label-sm` (0.6875rem). Use for "SKU" or "Timestamp" labels in `on-surface-variant`.

**Editorial Note:** Always pair a large `display-md` Rupiah value with a `label-md` descriptor in all-caps (e.g., "GROSS REVENUE") to mimic financial report aesthetics.

---

## 4. Elevation & Depth
Traditional shadows are prohibited. We use **Tonal Layering** and **Ambient Light**.

*   **The Layering Principle:** Place a `surface-container-lowest` card on a `surface-container-low` background to create a soft "lift" without any shadow at all.
*   **Ambient Shadows:** For floating elements (Modals/Popovers), use a highly diffused shadow:
    *   `box-shadow: 0 20px 40px -10px rgba(11, 28, 48, 0.06);` (Using a tint of `on-surface`).
*   **The Ghost Border:** If a separator is required for high-density inventory tables, use the `outline-variant` token at **15% opacity**. It should be felt, not seen.

---

## 5. Components & Data Density

### Buttons (The Interaction Points)
*   **Primary:** Emerald Gradient with `xl` (0.75rem) corners. High-contrast white text.
*   **Secondary:** `surface-container-high` background. No border.
*   **Ghost:** Transparent background, `primary` text. Use for "Add Note" or "Cancel."

### Input Fields (Smart POS Context)
*   **The Barcode Look:** Inventory search inputs should use `surface-container-low` with a `Ghost Border` that turns `primary` on focus.
*   **Currency Inputs:** Prefix with "Rp" in a `label-md` font weight, anchored to the left.

### Cards & Lists (The Core)
*   **Rule:** Forbid divider lines between list items. 
*   **Solution:** Use 12px vertical spacing. For inventory rows, use alternating subtle background shifts (`surface` vs `surface-container-low`) only if the data density exceeds 20 rows per screen.

### Smart POS Specifics:
*   **Quick-Action Chips:** For "Frequent Items," use `secondary-container` with `on-secondary-container` text. Corners: `full`.
*   **Inventory Status:** Instead of "In Stock" text, use a 6px Emerald dot (`primary`) next to the quantity.

---

## 6. Do’s and Don’ts

### Do
*   **Do** prioritize vertical rhythm over horizontal lines. Use white space as the primary separator.
*   **Do** format all currency as `Rp 1.000.000` using `tabular-nums` for alignment in tables.
*   **Do** use `rounded-xl` for cards and `rounded-md` for small utility buttons to create a "nested" visual language.

### Don’t
*   **Don’t** use pure black (#000000). Always use `on-surface` (#0b1c30) for text to maintain the Slate/Emerald sophistication.
*   **Don’t** use "Drop Shadows" on cards that are part of the main grid. Only use Tonal Layering.
*   **Don’t** use high-saturation red for errors. Use the `error` token (#ba1a1a) which is tuned to the Slate palette.

### Accessibility Note
Ensure that all Emerald text on White backgrounds meets a 4.5:1 contrast ratio. If necessary, use `primary` (#006c49) for text and `primary-container` (#10b981) for backgrounds.