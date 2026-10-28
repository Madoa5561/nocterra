"""NOCTERRA AI asset pipeline.

Standard-library-first Python package (Pillow + numpy optional) that generates,
validates, records and tracks AI assets used by the NOCTERRA mod.

Safety model (see ``docs/ASSET_PIPELINE.md``):
  * ``ASSET_DRY_RUN=true``  -> every billable request is built and printed as JSON,
                              never sent. No HTTP leaves the machine.
  * ``ASSET_AUTO_SPEND=false`` -> same as above even when DRY_RUN is false.
  * ``ASSET_MAX_COST_USD`` -> a hard ceiling checked *before* any request is sent.
  * API keys are only ever read from ``<repo>/.env`` and are always masked in output.
"""

__version__ = "0.1.0"
SCHEMA_VERSION = 1
