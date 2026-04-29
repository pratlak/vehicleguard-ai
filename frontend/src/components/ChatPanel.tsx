import React, { useState, useRef, useEffect } from 'react';
import {
  Button, Input, List, Typography, Card, Badge, Drawer, Spin, Alert, Space, Tag
} from 'antd';
import { MessageOutlined, SendOutlined, CloseOutlined, RobotOutlined, UserOutlined } from '@ant-design/icons';
import { sendChatMessage, assessRisk } from '../api/riskApi';
import { ChatResponse, AssessResponse } from '../types';

const { Text, Paragraph } = Typography;

interface Message {
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
}

interface ChatPanelProps {
  quoteResult: AssessResponse | null;
  onQuoteResult: (result: AssessResponse) => void;
}

const ChatPanel: React.FC<ChatPanelProps> = ({ quoteResult, onQuoteResult }) => {
  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState<Message[]>([]);
  const [inputValue, setInputValue] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [sessionId] = useState(() => crypto.randomUUID());
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<any>(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  useEffect(() => {
    if (open && messages.length === 0) {
      addAssistantMessage(
          "Hi! I'm VehicleGuard, your insurance assistant. I can help you get a premium estimate. Just tell me a bit about yourself and your vehicle. How old are you?"
      );
    }
  }, [open]);

  useEffect(() => {
    if (!loading) {
      inputRef.current?.focus();
    }
  }, [loading, messages]);

  const addAssistantMessage = (content: string) => {
    setMessages(prev => [...prev, { role: 'assistant', content, timestamp: new Date() }]);
  };

  const handleSend = async () => {
    if (!inputValue.trim() || loading) return;

    const userMessage = inputValue.trim();
    setInputValue('');
    setMessages(prev => [...prev, { role: 'user', content: userMessage, timestamp: new Date() }]);
    setLoading(true);
    setError(null);

    try {
      const quoteContext = quoteResult
          ? `Current quote: Risk tier ${quoteResult.riskTier}, Score ${quoteResult.riskScore}/100, ` +
          `Annual premium $${quoteResult.annualPremiumUsd}. Applied factors: ${
              quoteResult.appliedFactors.map(f => f.label).join(', ')}`
          : undefined;

      const response: ChatResponse = await sendChatMessage({
        sessionId,
        message: userMessage,
        quoteContext,
      });

      addAssistantMessage(response.reply);

      if (response.detectedAction === 'SUBMIT_QUOTE' && response.quoteData) {
        setTimeout(async () => {
          addAssistantMessage('Great! Let me calculate your quote now...');
          try {
            const quoteResp = await assessRisk(response.quoteData!);
            onQuoteResult(quoteResp);
            addAssistantMessage(
                `✅ Your quote is ready!\n\n` +
                `💰 Monthly Premium: $${quoteResp.monthlyPremiumUsd.toFixed(2)}\n` +
                `📊 Risk Score: ${quoteResp.riskScore}/100 (${quoteResp.riskTier})\n\n` +
                `${quoteResp.aiExplanation}\n\n` +
                `Close this chat to see your full breakdown.`
            );
          } catch {
            addAssistantMessage('I had trouble calculating your quote. Please try the form instead.');
          }
        }, 500);
      }
    } catch {
      setError('Failed to send message. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  return (
      <>
        <div style={{ position: 'fixed', bottom: 24, right: 24, zIndex: 1000 }}>
          <Badge count={messages.filter(m => m.role === 'assistant').length} overflowCount={9}>
            <Button
                type="primary"
                shape="circle"
                size="large"
                icon={<MessageOutlined />}
                onClick={() => setOpen(true)}
                style={{ width: 56, height: 56, fontSize: 20, boxShadow: '0 4px 12px rgba(0,0,0,0.2)' }}
            />
          </Badge>
        </div>

        <Drawer
            title={
              <Space>
                <RobotOutlined style={{ color: '#1677ff' }} />
                <span>VehicleGuard Assistant</span>
                <Tag color="blue">AI Powered</Tag>
              </Space>
            }
            placement="right"
            onClose={() => setOpen(false)}
            open={open}
            width={420}
            closeIcon={<CloseOutlined />}
            footer={
              <div style={{ display: 'flex', gap: 8 }}>
                <Input.TextArea
                    ref={inputRef}
                    value={inputValue}
                    onChange={e => setInputValue(e.target.value)}
                    onKeyDown={handleKeyPress}
                    placeholder="Ask me about your insurance..."
                    autoSize={{ minRows: 1, maxRows: 4 }}
                    disabled={loading}
                    style={{ flex: 1 }}
                />
                <Button
                    type="primary"
                    icon={<SendOutlined />}
                    onClick={handleSend}
                    loading={loading}
                    disabled={!inputValue.trim()}
                />
              </div>
            }
        >
          {error && (
              <Alert type="error" message={error} closable onClose={() => setError(null)}
                     style={{ marginBottom: 12 }} />
          )}

          <div style={{ height: 'calc(100vh - 220px)', overflowY: 'auto', paddingRight: 4 }}>
            <List
                dataSource={messages.filter(msg => !msg.content.includes('SUBMIT_QUOTE'))}
                renderItem={(msg) => (
                    <List.Item style={{ padding: '8px 0', border: 'none' }}>
                      <div style={{
                        display: 'flex',
                        justifyContent: msg.role === 'user' ? 'flex-end' : 'flex-start',
                        width: '100%',
                      }}>
                        <Card
                            size="small"
                            style={{
                              maxWidth: '80%',
                              background: msg.role === 'user' ? '#1677ff' : '#f5f5f5',
                              borderColor: msg.role === 'user' ? '#1677ff' : '#d9d9d9',
                            }}
                            bodyStyle={{ padding: '8px 12px' }}
                        >
                          <Space align="start" size={8}>
                            {msg.role === 'assistant' && (
                                <RobotOutlined style={{ color: '#1677ff', marginTop: 2 }} />
                            )}
                            <Paragraph
                                style={{
                                  margin: 0,
                                  color: msg.role === 'user' ? 'white' : 'inherit',
                                  whiteSpace: 'pre-wrap',
                                  wordBreak: 'break-word',
                                }}
                            >
                              {msg.content}
                            </Paragraph>
                            {msg.role === 'user' && (
                                <UserOutlined style={{ color: 'rgba(255,255,255,0.7)', marginTop: 2 }} />
                            )}
                          </Space>
                        </Card>
                      </div>
                    </List.Item>
                )}
            />
            {loading && (
                <div style={{ textAlign: 'center', padding: 12 }}>
                  <Spin size="small" />
                  <Text type="secondary" style={{ marginLeft: 8, fontSize: 12 }}>VehicleGuard is typing...</Text>
                </div>
            )}
            <div ref={messagesEndRef} />
          </div>
        </Drawer>
      </>
  );
};

export default ChatPanel;
