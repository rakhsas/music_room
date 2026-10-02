import { BadRequestException, ForbiddenException, HttpStatus } from '@nestjs/common';
import { assertPremium, cardBrand, chargeCard, luhnValid } from './subscription';

const now = new Date(2026, 8, 29); // 29 Sep 2026
const card = (overrides = {}) => ({ number: '4242 4242 4242 4242', expMonth: 12, expYear: 2030, cvc: '123', holderName: 'Jane', ...overrides });

describe('simulated checkout', () => {
  it('accepts a valid card and keeps only brand + last 4', () => {
    expect(chargeCard(card(), now)).toEqual({ brand: 'Visa', last4: '4242' });
  });

  it('runs the Luhn checksum', () => {
    expect(luhnValid('4242424242424242')).toBe(true);
    expect(luhnValid('4242424242424241')).toBe(false);
    expect(() => chargeCard(card({ number: '4242 4242 4242 4241' }), now)).toThrow(BadRequestException);
  });

  it('treats a card as valid through the end of its expiry month', () => {
    expect(() => chargeCard(card({ expMonth: 9, expYear: 2026 }), now)).not.toThrow();
    expect(() => chargeCard(card({ expMonth: 8, expYear: 2026 }), now)).toThrow('Card has expired');
  });

  it('checks the CVC length for the brand', () => {
    expect(() => chargeCard(card({ cvc: '12' }), now)).toThrow('CVC');
    expect(chargeCard(card({ number: '378282246310005', cvc: '1234' }), now).brand).toBe('Amex');
    expect(() => chargeCard(card({ number: '378282246310005', cvc: '123' }), now)).toThrow('CVC');
  });

  it('declines the decline test card with 402', () => {
    try {
      chargeCard(card({ number: '4000 0000 0000 0002' }), now);
      fail('expected a decline');
    } catch (e: any) {
      expect(e.getStatus()).toBe(HttpStatus.PAYMENT_REQUIRED);
    }
  });

  it('detects card brands', () => {
    expect(cardBrand('5555555555554444')).toBe('Mastercard');
    expect(cardBrand('2223003122003222')).toBe('Mastercard');
    expect(cardBrand('6011111111111117')).toBe('Card');
  });

  it('assertPremium only lets premium users through', () => {
    expect(() => assertPremium({ isPremium: false }, 'Private playlists')).toThrow(ForbiddenException);
    expect(() => assertPremium({ isPremium: true }, 'Private playlists')).not.toThrow();
  });
});
