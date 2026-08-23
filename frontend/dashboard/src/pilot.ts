import type {OperationalCapability} from './domain';

/**
 * Presentation-only data supplied for the current pilot.
 * These values are never used for authorization, ownership or API scope.
 */
export const pilotPresentation = {
  brandName: 'Money Makers',
  primaryContactName: 'Owen de Jesus',
} as const;

/**
 * The current dashboard is a browser-only Vite application. No secure server-owned
 * Merchant API credential boundary exists in frontend/dashboard, so pilot manual
 * acceptance must remain unavailable here. Backend remains final authority.
 */
export const pilotCapabilities: OperationalCapability = {
  canUsePilotManualAcceptance: false,
  source: 'UNAVAILABLE',
};
