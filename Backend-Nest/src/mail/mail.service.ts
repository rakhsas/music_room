import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import * as nodemailer from 'nodemailer';

@Injectable()
export class MailService {
  private readonly transporter;

  constructor(private readonly config: ConfigService) {
    this.transporter = nodemailer.createTransport({
      host: 'smtp.gmail.com',
      port: 587,
      secure: false,
      auth: {
        user: this.config.get<string>('EMAIL_HOST_USER'),
        pass: this.config.get<string>('EMAIL_HOST_PASSWORD'),
      },
    });
  }

  send(to: string, subject: string, text: string) {
    return this.transporter.sendMail({
      from: this.config.get<string>('EMAIL_HOST_USER'),
      to,
      subject,
      text,
    });
  }
}
